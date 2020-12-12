package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.DigestPathDataSource;
import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.Broker;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.util.Util;

@Singleton
public class BrokerImpl implements BrokerBackend, Broker {
	private static final Logger log = Logger.getLogger(BrokerImpl.class.getName());
	private static final String[] RESOURCE_DIGESTS = new String[]{"MD5","SHA-256"};

	private DataSource brokerDB;
	private Path dataDir; // for node resource data
	/**
	 * Character streams up to this size should be kept
	 * in memory for data transfers. Larger streams
	 * might be written to a temporary file which will
	 * be deleted automatically.
	 */
	private int inMemoryTreshold = 65536;
	
	private static final String SELECT_MEDIATYPE_BY_REQUESTID = "SELECT media_type FROM request_definitions WHERE request_id=?";

	public BrokerImpl(){
	}
	public BrokerImpl(DataSource brokerDB, Path dataDirectory) throws IOException{
		this();
		setBrokerDB(brokerDB);
		setDataDirectory(dataDirectory);
		Files.createDirectories(dataDirectory);
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#setBrokerDB(javax.sql.DataSource)
	 */
	@Override
	@Resource(name="brokerDB")
	public void setBrokerDB(DataSource brokerDB){
		this.brokerDB = brokerDB;
	}

	public void setDataDirectory(Path dataDir){
		this.dataDir = dataDir;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.AggregatorBackend#clearDataDirectory()
	 */
	@Override
	public void clearDataDirectory() throws IOException{
		try( Stream<Path> files = Files.list(dataDir) ){
			files.forEach( t -> {
				try {
					Files.delete(t);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} );
		}
	}
	
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#getAllNodes()
	 */
	@Override
	public List<Node> getAllNodes() throws SQLException{
		// TODO read/write nodes to external file
		List<Node> nl = new ArrayList<>();
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement st = dbc.prepareStatement("SELECT id, subject_dn, last_contact FROM nodes") )
		{
			// XXX maybe better to add an is_admin column
			ResultSet rs = st.executeQuery();
//			Statement st = dbc.createStatement();
//			ResultSet rs = st.executeQuery("SELECT id, subject_dn, last_contact FROM nodes");
			while( rs.next() ){
				// admins are not returned unless explicitly asked for
				if( Principal.isAdminDN(rs.getString(2)) ){
					continue;
				}
				nl.add(new Node(rs.getInt(1), rs.getString(2), rs.getTimestamp(3).toInstant()));
			}
			rs.close();
		}
		return nl;
	}

	@Override
	public Node getNode(int nodeId) throws SQLException{
		Node n;
		try( Connection dbc = brokerDB.getConnection() ){
			try( Statement st = dbc.createStatement() ){				
				ResultSet rs = st.executeQuery("SELECT id, subject_dn, last_contact FROM nodes WHERE id="+nodeId);
				if( rs.next() ){
					n = new Node(rs.getInt(1), rs.getString(2), rs.getTimestamp(3).toInstant());
				}else{
					n = null;
				}
				rs.close();
			}
			if( n != null ){
				// load module versions
				try( Statement st = dbc.createStatement() ){
					ResultSet rs = st.executeQuery("SELECT module, version FROM node_modules WHERE node_id="+nodeId);
					n.modules = new HashMap<>();
					while( rs.next() ){
						n.modules.put(rs.getString(1), rs.getString(2));
					}
					rs.close();
				}
			}
		}		
		return n;
	}
//	public int incrementSequence(Connection dbc, String sequence_name) throws SQLException{
//		try( Statement st = dbc.createStatement();
//				ResultSet rs = st.executeQuery("SELECT NEXTVAL('"+sequence_name+"')") ){
//			if( rs.next() ){
//				return rs.getInt(1);
//			}
//		}
//		throw new SQLException("Unable to increment sequence");
//	}
	protected int getLastInsertId(Connection dbc) throws SQLException{
		// TODO for other DBMS, the SQL will be different. E.g. sqlite
		// posgresql: SELECT LASTVAL()
		// hsql: CALL IDENTITY()
		try( Statement st = dbc.createStatement();
				ResultSet rs = st.executeQuery("CALL IDENTITY()") ){
			if( rs.next() ){
				return rs.getInt(1);
			}
		}
		throw new SQLException("Unable get last insert id");
	}

	private int createEmptyRequest(Connection dbc) throws SQLException{
		executeUpdate(dbc, "INSERT INTO requests(created) VALUES(NOW())");
		// get id
		return getLastInsertId(dbc);
		
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#createRequest(java.lang.String, java.io.Reader)
	 */
	@Override
	public int createRequest(String mediaType, Reader content) throws SQLException{
		int id;
		try( Connection dbc = brokerDB.getConnection() ){
			id = createEmptyRequest(dbc);
			// insert request content
			setRequestDefinition(dbc, id, mediaType, content);
			// commit transaction
			dbc.commit();
		}
		return id;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#createRequest(java.lang.String, java.io.Reader)
	 */
	@Override
	public int createRequest() throws SQLException{
		int id;
		try( Connection dbc = brokerDB.getConnection() ){
			id = createEmptyRequest(dbc);
			// commit transaction
			dbc.commit();
		}
		return id;
	}
	private void setRequestDefinition(Connection dbc, int requestId, String mediaType, Reader content) throws SQLException{
		// determine a definition is already there
		boolean hasRecord;
		try( PreparedStatement ps = dbc.prepareStatement("SELECT COUNT(*) FROM request_definitions WHERE request_id=? AND media_type=?") ){			
			ps.setInt(1, requestId);
			ps.setString(2, mediaType);
			ResultSet rs = ps.executeQuery();
			rs.next();
			hasRecord = (rs.getInt(1) != 0);
			rs.close();
		}

		if( hasRecord ){
			// already there, update the definition
			try( PreparedStatement ps = dbc.prepareStatement("UPDATE request_definitions SET query_def=? WHERE request_id=? AND media_type=?") ){
				ps.setClob(1, content);
				ps.setInt(2, requestId);
				ps.setString(3, mediaType);
				ps.executeUpdate();		
			}
			log.info("Updated definition for request "+requestId+": "+mediaType);
		}else{
			// no definition, create one
			try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO request_definitions (request_id, media_type, query_def) VALUES(?,?,?)") ){
				ps.setInt(1, requestId);
				ps.setString(2, mediaType);
				ps.setClob(3, content);
				ps.executeUpdate();
			}
			log.info("New definition for request "+requestId+": "+mediaType);
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#addRequestDefinition(int, java.lang.String, java.io.Reader)
	 */
	@Override
	public void setRequestDefinition(int requestId, String mediaType, Reader content) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){
			// TODO should also replace existing definitions
			setRequestDefinition(dbc, requestId, mediaType, content);	
			dbc.commit();
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#deleteRequest(int)
	 */
	@Override
	public void deleteRequest(int id) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){

			executeUpdate(dbc, "DELETE FROM request_definitions WHERE request_id="+id);

			executeUpdate(dbc, "DELETE FROM requests WHERE id="+id);
			// commit transaction
			dbc.commit();
		}
		log.info("Request "+id+" deleted");
	}
	@FunctionalInterface
	public interface RequestInfoLoader{
		RequestInfo load(ResultSet rs) throws SQLException;
	}
	private List<RequestInfo> loadRequestList(ResultSet rs, int mediaTypeIndex, RequestInfoLoader loader) throws SQLException{
		List<RequestInfo> l = new ArrayList<>();
		List<String> types = new ArrayList<>();
		int prevId = -1;
		RequestInfo req = null;
		while( rs.next() ){
			int id = rs.getInt(1);
			if( id != prevId ){
				//new request info
				if( req != null ){
					// previous one is complete, add to list
					req.setTypes(types.toArray(new String[types.size()]));
					l.add(req);
					types.clear();
				}
				req = loader.load(rs);
				// add type
				types.add(rs.getString(mediaTypeIndex));
			}else{
				// another type for the same request
				types.add(rs.getString(mediaTypeIndex));
			}
			// remember id for next row
			prevId = id;
		}
		// add last request
		if( req != null ){
			req.setTypes(types.toArray(new String[types.size()]));
			l.add(req);
		}
		return l;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#listAllRequests()
	 */
	@Override
	public List<RequestInfo> listAllRequests() throws SQLException{
		List<RequestInfo> list;
		try( Connection dbc = brokerDB.getConnection();
				Statement st = dbc.createStatement() )
		{
			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, d.media_type, r.targeted FROM requests r JOIN request_definitions d ON r.id=d.request_id ORDER BY r.id");
			list = loadRequestList(rs, 4,  
					r -> new RequestInfo(r.getInt(1), optionalTimestamp(rs, 2), optionalTimestamp(rs, 3), rs.getBoolean(5))
			);
			rs.close();
		}
		return list;
	}
	/**
	 * Convert SQL timestamp to instant with {@code null} support.
	 * @return instant or {@code null} if timestamp was null
	 * @throws SQLException error
	 */
	private Instant optionalTimestamp(ResultSet rs, int index) throws SQLException{
		Timestamp ts = rs.getTimestamp(index);
		return (ts==null)?null:ts.toInstant();
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#getRequestTypes(int)
	 */
	@Override
	public List<String> getRequestTypes(int requestId) throws SQLException{
		List<String> types = new ArrayList<>();
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement(SELECT_MEDIATYPE_BY_REQUESTID) )
		{
			ps.setInt(1, requestId);
			ResultSet rs = ps.executeQuery();
			while( rs.next() ){
				types.add(rs.getString(1));
			}
		}
		return types;
	}

	private Reader createTemporaryClobReader(Clob clob) throws SQLException, IOException{
		if( clob.length() < inMemoryTreshold ){
			String str = null;
			try( Reader reader = clob.getCharacterStream() ){
				str = Util.readContent(reader);
			}
			return new StringReader(str);
		}else{
			// current implementation reads the clob directly.
			// if this is implemented later asynchronously, we need to make sure that
			// the underlying connection can be closed and the clob is still readable.
			throw new UnsupportedOperationException("Temporary file CLOB reader not implemented yet. Size > "+inMemoryTreshold);
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#getRequestDefinition(int, java.lang.String)
	 */
	@Override
	public Reader getRequestDefinition(int requestId, String mediaType) throws SQLException, IOException{
		try( Connection dbc = brokerDB.getConnection();
			PreparedStatement ps = dbc.prepareStatement("SELECT query_def FROM request_definitions WHERE request_id=? AND media_type=?") )
		{
			ps.setInt(1, requestId);
			ps.setString(2, mediaType);
			ResultSet rs = ps.executeQuery();
			if( !rs.next() ){
				return null;
			}
			return createTemporaryClobReader(rs.getClob(1));
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#listRequestsForNode(int)
	 */
	@Override
	public List<RequestInfo> listRequestsForNode(int nodeId) throws SQLException{
		List<RequestInfo> list = new ArrayList<>();
		List<String> types = new ArrayList<>();
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement(SELECT_MEDIATYPE_BY_REQUESTID);
				Statement st = dbc.createStatement() )
		{
			// targeted requests are ONLY supplied to selected nodes: .. AND (r.targeted = FALSE OR s.request_id IS NOT NULL)
			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, r.targeted, s.retrieved, s.interaction, s.queued, s.processing, s.completed, s.rejected, s.failed FROM requests r LEFT OUTER JOIN request_node_status s ON r.id=s.request_id AND s.node_id="+ nodeId+" WHERE s.deleted IS NULL AND r.closed IS NULL AND r.published IS NOT NULL AND (r.targeted = FALSE OR s.request_id IS NOT NULL) ORDER BY r.id");
//			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, s.retrieved FROM requests r LEFT OUTER JOIN request_node_status s ON r.id=s.request_id AND s.node_id="+ nodeId+" WHERE s.deleted IS NULL AND r.closed IS NULL AND r.published IS NOT NULL ORDER BY r.id");
			while( rs.next() ){
				RequestInfo ri = new RequestInfo(rs.getInt(1), optionalTimestamp(rs, 2), optionalTimestamp(rs,3), rs.getBoolean(4));
				RequestStatusInfo status = new RequestStatusInfo();
				status.retrieved = optionalTimestamp(rs, 5);
				status.interaction = optionalTimestamp(rs, 6);
				status.queued = optionalTimestamp(rs, 7);
				status.processing = optionalTimestamp(rs, 8);
				status.completed = optionalTimestamp(rs, 9);
				status.rejected = optionalTimestamp(rs, 10);
				status.failed = optionalTimestamp(rs, 11);
				// TODO more status
				if( status.getStatus() == null ){
					// all timestamps empty, there is no status
					ri.nodeStatus = Collections.emptyList();
				}else{
					ri.nodeStatus = Collections.singletonList(status);
				}
				// deleted timestamp will always be null here, because of the where clause
				list.add(ri);
				// retrieve media types
				ps.setInt(1, rs.getInt(1));
				ps.clearWarnings();
				types.clear();
				ResultSet rs2 = ps.executeQuery();
				while( rs2.next() ){
					types.add(rs2.getString(1));
				}
				if( !types.isEmpty() ){
					ri.setTypes(types.toArray(new String[types.size()]));
				}
			}
			rs.close();
		}
		return list;		
	}
	
	@Override
	public RequestInfo getRequestInfo(int requestId) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){
			return loadRequest(dbc, requestId, true);
		}
	}
	
	private RequestInfo loadRequest(Connection dbc, int requestId, boolean fillTypes) throws SQLException{
		RequestInfo ri = null;
		try( Statement st = dbc.createStatement() ){
			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, r.targeted FROM requests r WHERE r.id="+requestId);
			if( rs.next() ){
				ri = new RequestInfo(rs.getInt(1), optionalTimestamp(rs, 2), optionalTimestamp(rs, 3), rs.getBoolean(4));
			}
			rs.close();
		}

		if( fillTypes ){
			// load request types
			try( PreparedStatement ps = dbc.prepareStatement(SELECT_MEDIATYPE_BY_REQUESTID) ){
				ps.setInt(1, requestId);
				ArrayList<String> types = new ArrayList<>();
				ResultSet rs = ps.executeQuery();
				while( rs.next() ){
					types.add(rs.getString(1));
				}
				if( !types.isEmpty() ){
					ri.setTypes(types.toArray(new String[types.size()]));
				}
			}
		}
		return ri;
	}
	private boolean loadRequestNodeStatus(Connection dbc, int nodeId, int requestId, RequestStatusInfo ri) throws SQLException{
		
		boolean status_found = false;
		try( Statement st = dbc.createStatement() ){
			ResultSet rs = st.executeQuery("SELECT retrieved, deleted FROM request_node_status r WHERE request_id="+requestId+" AND node_id="+nodeId);
			if( rs.next() ){
				status_found = true;
				ri.retrieved = optionalTimestamp(rs, 1);
				ri.deleted = optionalTimestamp(rs, 2);
				// don't need other time stamps,
				// this method is only used internally to find out if a status record exists 
				// and if the query was deleted
			}
			rs.close();
		}
		return status_found;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#setRequestNodeStatus(int, int, org.aktin.broker.xml.RequestStatus, java.time.Instant)
	 */
	@Override
	public void setRequestNodeStatus(int requestId, int nodeId, RequestStatus status, Instant timestamp) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){
			Timestamp ts = Timestamp.from(timestamp);
			int rowCount = 0;
			// no status message, just update the timestamp
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE request_node_status SET ").append(status.name()).append("=?");
			// for status other than interaction, clear the interaction timestamp
			if( status != RequestStatus.interaction ){
				sql.append(", interaction=NULL");
			}
			sql.append(" WHERE request_id=? AND node_id=?");
			try( PreparedStatement ps = dbc.prepareStatement(sql.toString()) ){
				ps.setTimestamp(1, ts);
				ps.setInt(2, requestId);
				ps.setInt(3, nodeId);
				rowCount = ps.executeUpdate();
			}
			// status message must be updated via a different method
			if( rowCount == 0 ){
				// row not there, insert row
				try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO request_node_status(request_id, node_id, "+status.name()+") VALUES(?,?,?)") ){
					ps.setInt(1, requestId);
					ps.setInt(2, nodeId);
					ps.setTimestamp(3, ts);
					ps.executeUpdate();
				}
			}
			dbc.commit();
		}
	}
	@Override
	public void setRequestNodeStatusMessage(int requestId, int nodeId, String mediaType, Reader message) throws SQLException{
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement("UPDATE request_node_status SET message_type=?, message=? WHERE request_id=? AND node_id=?") )
		{
			ps.setString(1, mediaType);
			ps.setClob(2, message);
			ps.setInt(3, requestId);
			ps.setInt(4, nodeId);
			ps.executeUpdate();
			dbc.commit();
		}
	}

	// TODO unit test
	@Override
	public Reader getRequestNodeStatusMessage(int requestId, int nodeId) throws SQLException, IOException{
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement("SELECT message_type, message FROM request_node_status WHERE request_id=? AND node_id=?") )
		{
			
			ps.setInt(1, requestId);
			ps.setInt(2, nodeId);
			ResultSet rs = ps.executeQuery();
			if( !rs.next() ){
				return null;
			}
			Clob clob = rs.getClob(2);
			if( clob == null ){
				return null;
			}

			return createTemporaryClobReader(clob);
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#markRequestDeletedForNode(int, int)
	 */
	@Override
	public boolean markRequestDeletedForNode(int nodeId, int requestId) throws SQLException{
		boolean delete_ok = false;
		try( Connection dbc = brokerDB.getConnection() ){
			RequestStatusInfo si = new RequestStatusInfo();
			RequestInfo ri = loadRequest(dbc, requestId, false);
			if( ri == null ){
				// nothing to do, not found
				//return false
			}else if( false == loadRequestNodeStatus(dbc, nodeId, requestId, si) ){
				// no status for node, need to insert
				// this also means that the request was never retrieved by the node
				try( Statement st = dbc.createStatement() ){
					st.executeUpdate("INSERT INTO request_node_status(deleted, node_id, request_id) VALUES(NOW(),"+nodeId+","+requestId+")");
				}
				dbc.commit();
				delete_ok = true;
			}else if( si.deleted == null ){
				// request was retrieved by node, but not deleted.
				// we need to update the timestamp to now
				try( Statement st = dbc.createStatement() ){
					st.executeUpdate("UPDATE request_node_status SET deleted=NOW() WHERE request_id="+requestId+" AND node_id="+nodeId);
				}
				dbc.commit();
				delete_ok = true;
			}else{
				// request was already deleted, nothing to do for us
				//return false;
			}
		}
		return delete_ok;
	}

	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#listRequestNodeStatus(java.lang.Integer)
	 */
	@Override
	public List<RequestStatusInfo> listRequestNodeStatus(Integer requestId) throws SQLException {
		List<RequestStatusInfo> list = new ArrayList<>();
		try( Connection dbc = brokerDB.getConnection();
				Statement st = dbc.createStatement() )
		{
			ResultSet rs = st.executeQuery("SELECT node_id, retrieved, deleted, queued, processing, completed, rejected, failed, interaction, message_type  FROM request_node_status WHERE request_id="+requestId);
			while( rs.next() ){
				RequestStatusInfo info = new RequestStatusInfo(rs.getInt(1));
				info.retrieved = optionalTimestamp(rs, 2);
				info.deleted = optionalTimestamp(rs, 3);
				info.queued = optionalTimestamp(rs, 4);
				info.processing = optionalTimestamp(rs, 5);
				info.completed = optionalTimestamp(rs, 6);
				info.rejected = optionalTimestamp(rs, 7);
				info.failed = optionalTimestamp(rs, 8);
				info.interaction = optionalTimestamp(rs, 9);
				info.type = rs.getString(10);
				list.add(info);
			}
			rs.close();
		}
		return list;
	}

	private Principal loadPrincipalByCertId(PreparedStatement ps, String certId) throws SQLException{
		ps.setString(1, certId);
		try( ResultSet rs = ps.executeQuery() ){
			if( rs.next() ){
				return new Principal(rs.getInt(1), rs.getString(2));
			}
		}
		return null;
	}

	public Principal accessPrincipal(String clientKey, String clientDn) throws SQLException{
		Principal p;
		try( Connection dbc = brokerDB.getConnection() ){
			// try to load from database
			PreparedStatement select_node = dbc.prepareStatement("SELECT id, subject_dn FROM nodes WHERE client_key=?");
			p = loadPrincipalByCertId(select_node, clientKey);
			if( p == null ){
				// insert into database
				try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO nodes(client_key, subject_dn, last_contact)VALUES(?,?,NOW())") ){
					ps.setString(1, clientKey);
					ps.setString(2, clientDn);
					ps.executeUpdate();					
				}
				// retrieve id
				select_node.clearParameters();
				p = loadPrincipalByCertId(select_node, clientKey);
			}else{
				// update last contact
				executeUpdate(dbc, "UPDATE nodes SET last_contact=NOW() WHERE id="+p.getNodeId());
			}
			select_node.close();
			dbc.commit();
		}
		return p;
	}
	private static void executeUpdate(Connection dbc, String sql) throws SQLException {
		try( Statement st = dbc.createStatement() ){
			st.executeUpdate(sql);
		}
	}


//	private static final String convertNodeToString(org.w3c.dom.Node node) throws TransformerException{
//	    TransformerFactory tf = TransformerFactory.newInstance();
//	    Transformer transformer = tf.newTransformer();
//
//	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//	    transformer.setOutputProperty(OutputKeys.INDENT, "no");
//	    StringWriter w = new StringWriter();
//	    transformer.transform(new DOMSource(node), new StreamResult(w));
//
//	    return w.toString();
//	}
	private void updateRequestTimestamp(Connection dbc, int requestId, String timestampColumn, Instant value) throws SQLException{
		try( PreparedStatement ps = dbc.prepareStatement("UPDATE requests SET "+timestampColumn+"=? WHERE id=?") ){	
			if( value != null ){
				ps.setTimestamp(1, Timestamp.from(value));
			}else{
				ps.setTimestamp(1, null);
			}
			ps.setInt(2, requestId);
			ps.executeUpdate();
		}
	}
	@Override
	public void setRequestPublished(int requestId, Instant timestamp) throws SQLException {
		try( Connection dbc = brokerDB.getConnection() ){
			updateRequestTimestamp(dbc, requestId, "published", timestamp);
			dbc.commit();
		}
	}
	@Override
	public void setRequestClosed(int requestId, Instant timestamp) throws SQLException {
		try( Connection dbc = brokerDB.getConnection() ){
			updateRequestTimestamp(dbc, requestId, "closed", timestamp);
			dbc.commit();
		}
	}
	@Override
	public void updateNodeLastSeen(int[] nodeIds, long[] timestamps) throws SQLException{
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement("UPDATE nodes SET last_contact=? WHERE id=?") )
		{
			for( int i=0; i<nodeIds.length; i++ ){
				ps.setInt(1, nodeIds[i]);
				ps.setTimestamp(2, new Timestamp(timestamps[i]));
				ps.executeUpdate();
			}
		}
	}
	@Override
	public void setRequestTargets(int requestId, int[] nodes) throws SQLException {
		Objects.requireNonNull(nodes);
		try( Connection dbc = brokerDB.getConnection() ){
			// set targeted
			executeUpdate(dbc, "UPDATE requests SET targeted=TRUE WHERE id="+requestId);

			// clear nodes
			// TODO issue warning, if the request was already retrieved by a node
			// TODO 
			executeUpdate(dbc, "DELETE FROM request_node_status WHERE request_id="+requestId);

			// insert target nodes
			try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO request_node_status(request_id,node_id)VALUES(?,?)") ){				
				ps.setInt(1, requestId);
				for( int i=0; i<nodes.length; i++ ){
					ps.setInt(2, nodes[i]);
					ps.executeUpdate();
				}
			}

			// commit transaction
			dbc.commit();
		}
	}
	@Override
	public int[] getRequestTargets(int requestId) throws SQLException {
		int[] nodes = null;
		try( Connection dbc = brokerDB.getConnection() ){
			// first find out, if the query is targeted at all
			boolean isTargeted = false;
			try( Statement st = dbc.createStatement() ){
				ResultSet rs = st.executeQuery("SELECT targeted FROM requests WHERE id="+requestId);
				if( rs.next() ){
					isTargeted = rs.getBoolean(1);
				}
			}
			if( isTargeted == true ){
				// retrieve nodes
				ArrayList<Integer> list = new ArrayList<>();
				try( Statement st = dbc.createStatement() ){
					ResultSet rs = st.executeQuery("SELECT node_id FROM request_node_status WHERE request_id="+requestId);
					while( rs.next() ){
						list.add(rs.getInt(1));
					}
					rs.close();
				}
				// fill array
				nodes = new int[list.size()];
				for( int i=0; i<list.size(); i++ ){
					nodes[i] = list.get(i);
				}
			}
		}
		return nodes;
	}
	@Override
	public void clearRequestTargets(int requestId) throws SQLException {
		try( Connection dbc = brokerDB.getConnection() ){
			executeUpdate(dbc, "UPDATE requests SET targeted=FALSE WHERE id="+requestId);
		}
	}
	private String nodeResourceName(int nodeId, String resourceId, MediaType mediaType){
		// use media type to generate file extension (e.g. .txt, .xml)
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(nodeId).append('_').append(URLEncoder.encode(resourceId, "UTF-8"));
			if( mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) ){
				builder.append(".txt");
			}else if( mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) || mediaType.getSubtype().endsWith("+xml") ){
				builder.append(".xml");
			}
			return builder.toString();
			
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	
	// replace file and generate checksum
	private byte[][] replaceResourceFile(InputStream data, String oldFile, String newFile) throws IOException{
		// replace file
		if( oldFile != null ){
			// delete old file
			try{
				Files.delete(dataDir.resolve(oldFile));
			}catch( IOException e ){
				// delete may fail, log warning
				log.log(Level.WARNING, "Unable to delete node resource: "+oldFile, e);
			}
		}
		// create the new file, shouldn't replace anything
		DigestCalculatingInputStream di;
		try {
			di = new DigestCalculatingInputStream(data, RESOURCE_DIGESTS);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("message digest SHA-256 not available");
		}
		Files.copy(di, dataDir.resolve(newFile));
		return di.getDigests();
	}
	@Override
	// TODO add last modified timestamp
	public void updateNodeResource(int nodeId, String resourceId, MediaType mediaType, InputStream content) throws IOException, SQLException {
		// TODO find if resource already exists
		String oldFile = null;
		String newFile = nodeResourceName(nodeId, resourceId, mediaType);
		try( Connection dbc = brokerDB.getConnection() ){
			try( PreparedStatement ps = dbc.prepareStatement("SELECT data_file FROM node_resources WHERE node_id=? AND name=?") ){				
				ps.setInt(1, nodeId);
				ps.setString(2, resourceId);
				ResultSet rs = ps.executeQuery();
				if( rs.next() ){
					oldFile = rs.getString(1);
				}
			}

			// replace file
			byte[][] digests = replaceResourceFile(content, oldFile, newFile);
			// XXX this is not 100% transaction safe, the file is still replaced if the next database operation fails. Would be better to backup the old file and restore it if the database operation fails

			// update database entry
			PreparedStatement ps = null;
			try{
				if( oldFile != null ){
					// previous entry, replace existing
					ps = dbc.prepareStatement("UPDATE node_resources SET media_type=?, last_modified=?, data_file=?, data_md5=?, data_sha2=? WHERE node_id=? AND name=?");
				}else{
					// no previous entry, create new one
					ps = dbc.prepareStatement("INSERT INTO node_resources(media_type, last_modified, data_file, data_md5, data_sha2, node_id, name)VALUES(?,?,?,?,?,?,?)");
				}
	
				ps.setString(1, mediaType.toString());
				ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				ps.setString(3, newFile);
				ps.setBytes(4, digests[0]);
				ps.setBytes(5, digests[1]);
				ps.setInt(6, nodeId);
				ps.setString(7, resourceId);
				ps.executeUpdate();
			}finally {
				if( ps != null ) {
					ps.close();					
				}
			}
			// done
			dbc.commit();
		}
	}
	@Override
	public DigestPathDataSource getNodeResource(int nodeId, String resourceId) throws SQLException{
		String mediaType;
		Instant lastModified;
		Path file;
		byte[] md5;
		byte[] sha2;
		try( Connection dbc = brokerDB.getConnection();
				PreparedStatement ps = dbc.prepareStatement("SELECT media_type, last_modified, data_file, data_md5, data_sha2 FROM node_resources WHERE node_id=? AND name=?")	){
			ps.setInt(1, nodeId);
			ps.setString(2, resourceId);
			ResultSet rs = ps.executeQuery();
			if( !rs.next() ){
				// not found
				return null;
			}
			mediaType = rs.getString(1);
			lastModified = rs.getTimestamp(2).toInstant();
			file = dataDir.resolve(rs.getString(3));
			md5 = rs.getBytes(4);
			sha2 = rs.getBytes(5);
			rs.close();
		}
		
		DigestPathDataSource ds = new DigestPathDataSource(file, mediaType, lastModified);
		ds.md5 = md5;
		ds.sha256 = sha2;
		return ds;
	}

	/**
	 * Update the clientDN string for the nodes given in the provided map.
	 * @param ds data source
	 * @param mapNodeDN Map of node keys pointing to clientDN strings
	 * @return number of updated nodes
	 * @throws SQLException SQL error
	 */
	public static int updatePrincipalDN(DataSource ds, Map<String, String> mapNodeDN) throws SQLException {
		int updated = 0;
		try( Connection dbc = ds.getConnection();
				PreparedStatement update_dn = dbc.prepareStatement("UPDATE nodes SET subject_dn=? WHERE client_key=?") )
		{
			for( Entry<String, String> entry : mapNodeDN.entrySet() ){
				// try to update database
				update_dn.setString(1, entry.getValue());
				update_dn.setString(2, entry.getKey());
				updated += update_dn.executeUpdate();
			}
		}
		return updated;
	}
}
