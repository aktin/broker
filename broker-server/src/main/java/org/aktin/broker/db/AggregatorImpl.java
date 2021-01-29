package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.util.PathDataSource;
import org.aktin.broker.xml.ResultInfo;

@Singleton
public class AggregatorImpl implements AggregatorBackend {
	private DataSource ds;
	private Path dataDir;

	public AggregatorImpl() throws IOException{
		setDataDirectory(Paths.get("aggregator-data"));
		
	}
	public AggregatorImpl(DataSource ds, Path dataDir) throws IOException{
		setDataDirectory(dataDir);
		setBrokerDB(ds);
	}

	public void setDataDirectory(Path dataDir) throws IOException{
		this.dataDir = dataDir;
		// create dir if not existing
		Files.createDirectories(dataDir);
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.AggregatorBackend#setBrokerDB(javax.sql.DataSource)
	 */
	@Override
	@Resource(name="brokerDB")
	public void setBrokerDB(DataSource ds){
		this.ds = ds;
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
	private void removeData(String data_file) throws IOException{
		// we need to delete the file
		Path file = dataDir.resolve(data_file);
		Files.delete(file);
	}
	private String getFileExtension(MediaType mediaType){
		if( mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) ){
			return ".xml";
		}else if( mediaType.getType().equals("text") ){
			return ".txt";
		}else{
			return ".bin";
		}
	}
	/**
	 * Read the provided data and return a filename
	 * TODO use filename and return size
	 * @param requestId request id
	 * @param nodeId node id
	 * @param mediaType media type
	 * @param content content
	 * @return file name
	 * @throws IOException error
	 */
	private String readData(int requestId, int nodeId, MediaType mediaType, InputStream content) throws IOException{
		// for the prototype, always write to file
		String name = "result-"+requestId+"-"+nodeId+getFileExtension(mediaType);
		Path file = dataDir.resolve(name);
		//
		Files.copy(content, file);
		return name;
	}

	/* (non-Javadoc)
	 * @see org.aktin.broker.db.AggregatorBackend#listResults(int)
	 */
	@Override
	public List<ResultInfo> listResults(int requestId) throws SQLException{
		List<ResultInfo> list = new ArrayList<>();
		try( Connection dbc = ds.getConnection(); 
				Statement st = dbc.createStatement() ){
			// find is result is already present
			ResultSet rs = st.executeQuery("SELECT node_id, media_type FROM request_node_results WHERE request_id="+requestId);
			// compile list
			while( rs.next() ){
				list.add(new ResultInfo(rs.getString(1), rs.getString(2)));
			}
			rs.close();
		}
		return list;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.AggregatorBackend#getResult(int, int)
	 */
	@Override
	public DateDataSource getResult(int requestId, int nodeId) throws SQLException{
		DateDataSource data;
		try( Connection dbc = ds.getConnection(); 
				Statement st = dbc.createStatement() ){
			// find is result is already present
			ResultSet rs = st.executeQuery("SELECT media_type, last_modified, data_file FROM request_node_results WHERE request_id="+requestId+" AND node_id="+nodeId);
			if( rs.next() ){
				Timestamp ts = rs.getTimestamp(2);
				data = new PathDataSource(dataDir.resolve(rs.getString(3)), rs.getString(1), (ts==null)?null:ts.toInstant());
			}else{
				data = null;
			}
			rs.close();
		}
		return data;
	}
	/* (non-Javadoc)
	 * @see org.aktin.broker.db.AggregatorBackend#addOrReplaceResult(int, int, javax.ws.rs.core.MediaType, java.io.InputStream)
	 */
	@Override
	public void addOrReplaceResult(int requestId, int nodeId, MediaType mediaType, InputStream content) throws SQLException{
		try( Connection dbc = ds.getConnection();
				Statement st = dbc.createStatement() ){
			// find is result is already present
			ResultSet rs = st.executeQuery("SELECT media_type, data_file FROM request_node_results WHERE request_id="+requestId+" AND node_id="+nodeId);
			String insertOrUpdate;
			if( rs.next() ){
				String prevType = rs.getString(1);
				// remove data file if necessary
				// TODO log info: replacing prevType
				Objects.requireNonNull(prevType); // prevent unused code warning
				try{
					removeData(rs.getString(2));
				}catch( IOException e ){
					// TODO log error
				}
				// update data
				insertOrUpdate = "UPDATE request_node_results SET media_type=?, data_file=?, last_modified=NOW() WHERE request_id=? AND node_id=?";
			}else{
				// insert data
				insertOrUpdate = "INSERT INTO request_node_results (media_type, data_file, request_id, node_id, first_received, last_modified) VALUES(?,?,?,?, NOW(), NOW())";
			}
			rs.close();
			
			String file;
			try {
				file = readData(requestId, nodeId, mediaType, content);
				content.close();
			} catch (IOException e) {
				throw new SQLException("Unable to read supplied data", e);
			}
			try( PreparedStatement ps = dbc.prepareStatement(insertOrUpdate) ){
				ps.setString(1, mediaType.toString());
				ps.setString(2, file);
				ps.setInt(3, requestId);
				ps.setInt(4, nodeId);
				ps.executeUpdate();
			}
			dbc.commit();
		}
	}
	@Override
	public String[] getDistinctResultTypes(int requestId) throws SQLException {
		List<String> list = new ArrayList<>();
		try( Connection dbc = ds.getConnection();
				Statement st = dbc.createStatement() ){
			// find is result is already present
			ResultSet rs = st.executeQuery("SELECT DISTINCT media_type FROM request_node_results WHERE request_id="+requestId);
			
			// compile list
			while( rs.next() ){
				list.add(rs.getString(1));
			}
			rs.close();
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	public boolean isRequestWritable(int requestId, int nodeId) {
		// TODO check in database
		return true;
	}

}
