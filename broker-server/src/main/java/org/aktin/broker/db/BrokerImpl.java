package org.aktin.broker.db;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.Broker;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.SoftwareModule;
import org.aktin.broker.xml.util.Util;

@Singleton
public class BrokerImpl implements BrokerBackend, Broker {
	private static final Logger log = Logger.getLogger(BrokerImpl.class.getName());
	private DataSource brokerDB;
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
	public BrokerImpl(DataSource brokerDB){
		this();
		setBrokerDB(brokerDB);
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#setBrokerDB(javax.sql.DataSource)
	 */
	@Override
	@Resource(name="brokerDB")
	public void setBrokerDB(DataSource brokerDB){
		this.brokerDB = brokerDB;
	}
	
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#getAllNodes()
	 */
	@Override
	public List<Node> getAllNodes() throws SQLException{
		List<Node> nl = new ArrayList<>();
		try( Connection dbc = brokerDB.getConnection() ){
			// XXX maybe better to add an is_admin column
			PreparedStatement st = dbc.prepareStatement("SELECT id, subject_dn, last_contact FROM nodes");
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
			st.close();
		}
		return nl;
	}

	@Override
	public Node getNode(int nodeId) throws SQLException{
		Node n;
		try( Connection dbc = brokerDB.getConnection() ){
			Statement st = dbc.createStatement();
			ResultSet rs = st.executeQuery("SELECT id, subject_dn, last_contact FROM nodes WHERE id="+nodeId);
			if( rs.next() ){
				n = new Node(rs.getInt(1), rs.getString(2), rs.getTimestamp(3).toInstant());
			}else{
				n = null;
			}
			rs.close();
			st.close();
			if( n != null ){
				// load module versions
				st = dbc.createStatement();
				rs = st.executeQuery("SELECT module, version FROM node_modules WHERE node_id="+nodeId);
				n.modules = new HashMap<>();
				while( rs.next() ){
					n.modules.put(rs.getString(1), rs.getString(2));
				}
				rs.close();
				st.close();
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
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#createRequest(java.lang.String, java.io.Reader)
	 */
	@Override
	public int createRequest(String mediaType, Reader content) throws SQLException{
		int id;
		try( Connection dbc = brokerDB.getConnection() ){
			Statement st = dbc.createStatement();
			st.executeUpdate("INSERT INTO requests(created) VALUES(NOW())");
			st.close();
			// get id
			id = getLastInsertId(dbc);

			// insert request content
			setRequestDefinition(dbc, id, mediaType, content);
			// commit transaction
			dbc.commit();
		}
		return id;
	}
	private void setRequestDefinition(Connection dbc, int requestId, String mediaType, Reader content) throws SQLException{
		// determine a definition is already there
		PreparedStatement ps = dbc.prepareStatement("SELECT COUNT(*) FROM request_definitions WHERE request_id=? AND media_type=?");
		ps.setInt(1, requestId);
		ps.setString(2, mediaType);
		ResultSet rs = ps.executeQuery();
		rs.next();
		boolean hasRecord = (rs.getInt(1) != 0);
		rs.close();
		ps.close();
		ps = null;
		if( hasRecord ){
			// already there, update the definition
			ps = dbc.prepareStatement("UPDATE request_definitions SET query_def=? WHERE request_id=? AND media_type=?");
			ps.setClob(1, content);
			ps.setInt(2, requestId);
			ps.setString(3, mediaType);
			ps.executeUpdate();		
			ps.close();
			log.info("Updated definition for request "+requestId+": "+mediaType);
		}else{
			// no definition, create one
			ps = dbc.prepareStatement("INSERT INTO request_definitions (request_id, media_type, query_def) VALUES(?,?,?)");
			ps.setInt(1, requestId);
			ps.setString(2, mediaType);
			ps.setClob(3, content);
			ps.executeUpdate();
			ps.close();
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
			PreparedStatement ps = dbc.prepareStatement("DELETE FROM request_definitions WHERE request_id=?");
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();
			
			ps = dbc.prepareStatement("DELETE FROM requests WHERE id=?");
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();
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
		try( Connection dbc = brokerDB.getConnection() ){
			Statement st = dbc.createStatement();
			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, d.media_type FROM requests r JOIN request_definitions d ON r.id=d.request_id ORDER BY r.id");
			list = loadRequestList(rs, 4,  
					r -> new RequestInfo(Integer.toString(r.getInt(1)), optionalTimestamp(rs, 2), optionalTimestamp(rs, 3))
			);
			rs.close();
			st.close();
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
		try( Connection dbc = brokerDB.getConnection() ){
			PreparedStatement ps = dbc.prepareStatement(SELECT_MEDIATYPE_BY_REQUESTID);
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
			throw new UnsupportedOperationException("Temporary file CLOB reader not implemented yet. Size > "+inMemoryTreshold);
		}
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.BrokerBackend#getRequestDefinition(int, java.lang.String)
	 */
	@Override
	public Reader getRequestDefinition(int requestId, String mediaType) throws SQLException, IOException{
		try( Connection dbc = brokerDB.getConnection() ){
			PreparedStatement ps = dbc.prepareStatement("SELECT query_def FROM request_definitions WHERE request_id=? AND media_type=?");
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
		try( Connection dbc = brokerDB.getConnection() ){
			PreparedStatement ps = dbc.prepareStatement(SELECT_MEDIATYPE_BY_REQUESTID);
			Statement st = dbc.createStatement();
			ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed, s.retrieved FROM requests r LEFT OUTER JOIN request_node_status s ON r.id=s.request_id AND s.node_id="+ nodeId+" WHERE s.deleted IS NULL AND r.closed IS NULL AND r.published IS NOT NULL ORDER BY r.id");
			while( rs.next() ){
				RequestInfo ri = new RequestInfo(Integer.toString(rs.getInt(1)), optionalTimestamp(rs, 2), optionalTimestamp(rs,3));
				RequestStatusInfo status = new RequestStatusInfo();
				status.retrieved = optionalTimestamp(rs, 4);
				// TODO more status
				ri.nodeStatus = Collections.singletonList(status);
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
			st.close();
		}
		return list;		
	}
	
	@Override
	public RequestInfo getRequestInfo(int requestId) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){
			return loadRequest(dbc, requestId);
		}
	}
	
	private RequestInfo loadRequest(Connection dbc, int requestId) throws SQLException{
		Statement st = dbc.createStatement();
		ResultSet rs = st.executeQuery("SELECT r.id, r.published, r.closed FROM requests r WHERE r.id="+requestId);
		RequestInfo ri = null;
		if( rs.next() ){
			ri = new RequestInfo(Integer.toString(rs.getInt(1)), optionalTimestamp(rs, 2), optionalTimestamp(rs, 3));
		}
		rs.close();
		st.close();
		return ri;
	}
	private boolean loadRequestNodeStatus(Connection dbc, int nodeId, int requestId, RequestStatusInfo ri) throws SQLException{
		
		Statement st = dbc.createStatement();
		ResultSet rs = st.executeQuery("SELECT retrieved, deleted FROM request_node_status r WHERE request_id="+requestId+" AND node_id="+nodeId);
		boolean status_found = false;
		if( rs.next() ){
			status_found = true;
			ri.retrieved = optionalTimestamp(rs, 1);
			ri.deleted = optionalTimestamp(rs, 2);
			// don't need other time stamps,
			// this method is only used internally to find out if a status record exists 
			// and if the query was deleted
		}
		rs.close();
		st.close();
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
			PreparedStatement ps = dbc.prepareStatement("UPDATE request_node_status SET "+status.name()+"=? WHERE request_id=? AND node_id=?");
			ps.setTimestamp(1, ts);
			ps.setInt(2, requestId);
			ps.setInt(3, nodeId);
			rowCount = ps.executeUpdate();
			ps.close();
			// status message must be updated via a different method
			if( rowCount == 0 ){
				// row not there, insert row
				ps = dbc.prepareStatement("INSERT INTO request_node_status(request_id, node_id, "+status.name()+") VALUES(?,?,?)");
				ps.setInt(1, requestId);
				ps.setInt(2, nodeId);
				ps.setTimestamp(3, ts);
				ps.executeUpdate();
				ps.close();
			}
			dbc.commit();
		}
	}
	@Override
	public void setRequestNodeStatusMessage(int requestId, int nodeId, String mediaType, Reader message) throws SQLException{
		try( Connection dbc = brokerDB.getConnection() ){
			PreparedStatement ps = dbc.prepareStatement("UPDATE request_node_status SET message_type=?, message=? WHERE request_id=? AND node_id=?");
			ps.setString(1, mediaType);
			ps.setClob(2, message);
			ps.setInt(3, requestId);
			ps.setInt(4, nodeId);
			ps.executeUpdate();
			ps.close();
			dbc.commit();
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
			RequestInfo ri = loadRequest(dbc, requestId);
			if( ri == null ){
				// nothing to do, not found
				//return false
			}else if( false == loadRequestNodeStatus(dbc, nodeId, requestId, si) ){
				// no status for node, need to insert
				// this also means that the request was never retrieved by the node
				Statement st = dbc.createStatement();
				st.executeUpdate("INSERT INTO request_node_status(deleted, node_id, request_id) VALUES(NOW(),"+nodeId+","+requestId+")");
				st.close();
				dbc.commit();
				delete_ok = true;
			}else if( si.deleted == null ){
				// request was retrieved by node, but not deleted.
				// we need to update the timestamp to now
				Statement st = dbc.createStatement();
				st.executeUpdate("UPDATE request_node_status SET deleted=NOW() WHERE request_id="+requestId+" AND node_id="+nodeId);
				st.close();
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
		try( Connection dbc = brokerDB.getConnection() ){
			Statement st = dbc.createStatement();
			ResultSet rs = st.executeQuery("SELECT node_id, retrieved, deleted, queued, processing, completed, rejected, failed, message_type  FROM request_node_status WHERE request_id="+requestId);
			while( rs.next() ){
				RequestStatusInfo info = new RequestStatusInfo(rs.getInt(1));
				info.retrieved = optionalTimestamp(rs, 2);
				info.deleted = optionalTimestamp(rs, 3);
				info.queued = optionalTimestamp(rs, 4);
				info.processing = optionalTimestamp(rs, 5);
				info.completed = optionalTimestamp(rs, 6);
				info.rejected = optionalTimestamp(rs, 7);
				info.failed= optionalTimestamp(rs, 8);
				info.type = rs.getString(9);
				list.add(info);
			}
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
				PreparedStatement ps = dbc.prepareStatement("INSERT INTO nodes(client_key, subject_dn, last_contact)VALUES(?,?,NOW())");
				ps.setString(1, clientKey);
				ps.setString(2, clientDn);
				ps.executeUpdate();
				ps.close();
				// retrieve id
				select_node.clearParameters();
				p = loadPrincipalByCertId(select_node, clientKey);
			}else{
				// update last contact
				Statement st = dbc.createStatement();
				st.executeUpdate("UPDATE nodes SET last_contact=NOW() WHERE id="+p.getNodeId());
				st.close();
			}
			select_node.close();
			dbc.commit();
		}
		return p;
	}
	@Override
	public void updateNodeModules(int nodeId, List<SoftwareModule> modules) throws SQLException {
		try( Connection dbc = brokerDB.getConnection() ){
			// delete previous data
			Statement st = dbc.createStatement();
			st.executeUpdate("DELETE FROM node_modules WHERE node_id="+nodeId);
			st.close();
			// write new data
			PreparedStatement ps = dbc.prepareStatement("INSERT INTO node_modules(node_id, module, version)VALUES(?,?,?)");
			ps.setInt(1, nodeId);
			for( SoftwareModule m : modules ){
				ps.setString(2, m.getId());
				ps.setString(3, m.getVersion());
				ps.executeUpdate();
			}
			ps.close();
			dbc.commit();
		}		
	}

	private void updateRequestTimestamp(Connection dbc, int requestId, String timestampColumn, Instant value) throws SQLException{
		PreparedStatement ps = dbc.prepareStatement("UPDATE requests SET "+timestampColumn+"=? WHERE id=?");
		if( value != null ){
			ps.setTimestamp(1, Timestamp.from(value));
		}else{
			ps.setTimestamp(1, null);
		}
		ps.setInt(2, requestId);
		ps.executeUpdate();
		ps.close();
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
		try( Connection dbc = brokerDB.getConnection() ){
			PreparedStatement ps = dbc.prepareStatement("UPDATE nodes SET last_contact=? WHERE id=?");
			for( int i=0; i<nodeIds.length; i++ ){
				ps.setInt(1, nodeIds[i]);
				ps.setTimestamp(2, new Timestamp(timestamps[i]));
				ps.executeUpdate();
			}
			ps.close();
		}
	}
}
