package org.aktin.broker.query.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;

public class Execution implements Runnable{
	private static final Logger log = Logger.getLogger(Execution.class.getName());

	private Connection dbc;
	private SQLQuery query;
	private List<String> batch;

	public Execution(Connection dbc, SQLQuery query){
		this.dbc = dbc;
		this.query = query;
	}
	protected void handleWarning(String sql, SQLWarning warning){
		Objects.requireNonNull(warning);
		// handle warnings same as errors (without aborting)
		while( warning != null ){
			handleException(sql, warning);
			warning = warning.getNextWarning();
		}
	}

	protected void handleException(String sql, SQLException e){
		// TODO pass to caller
		log.warning("SQL error: "+e.getMessage());		
	}

	/**
	 * Map for property substitution in SQL statements
	 * @param properties lookup table
	 * @throws SubstitutionError 
	 */
	void prepareStatements(Function<String,String> propertyLookup) throws SubstitutionError{
		batch = new ArrayList<>();
		for( Source source : query.source ){
			source.splitStatements(propertyLookup, batch::add);
		}
	}
	private void runStatements() throws SQLException{
		for( String sql : batch ){
			SQLWarning warning = null;
			try( Statement stmt = dbc.createStatement() ){
				stmt.executeUpdate(sql);
				warning = stmt.getWarnings();
			}
			if( warning != null ){
				handleWarning(sql, warning);
			}
		}
	}

	public void cleanup(){
		// TODO method to drop temporary tables which were not exported
		for( ExportTable table : query.export ){
			String sql = "DROP TEMPORARY TABLE "+table.table+" IF EXISTS";
			try( Statement s = dbc.createStatement() ){
				s.executeQuery(sql);
			} catch (SQLException e) {
				handleException(sql, e);
			}
		}
	}

	private void exportTable(ExportTable export, TableWriter writer) throws SQLException, IOException{
		Statement s = dbc.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		String[] row;
		try( ResultSet rs = s.executeQuery("SELECT * FROM "+export.table) ){
			// write column headers
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			row = new String[count];
			for( int i=0; i<count; i++ ){
				row[i] = meta.getColumnLabel(i+1);
			}
			writer.header(row);
			// write data rows
			while( rs.next() ){
				for( int i=0; i<count; i++ ){
					Object o = rs.getObject(i+1);
					if( o == null ){
						row[i] = null;
					}else{
						// XXX maybe some formatting for date/time columns
						row[i] = o.toString();
					}
				}
				writer.row(row);
			}
		}
		s.close();
		writer.close();
	}
	
	private void anonymizeReference(List<String> batch, TableColumn reference){
		// add column to original table
		StringBuilder sql = new StringBuilder();
		sql.append("ALTER TABLE ").append(reference.table);
		sql.append(" ADD COLUMN a_").append(reference.column);
		sql.append("(INTEGER NULL)");
		batch.add(sql.toString());
		// fill column in original table
		sql = new StringBuilder();
		sql.append("UPDATE ").append(reference.table);
		sql.append(" SET a_").append(reference.column);
		sql.append(" = map.target ");
		sql.append(" FROM anon_map map WHERE ");
		sql.append(reference.column).append(" = map.id");
		batch.add(sql.toString());
		// drop original column
		sql = new StringBuilder();
		sql.append("ALTER TABLE ").append(reference.table);
		sql.append(" DROP COLUMN ").append(reference.column);
		batch.add(sql.toString());
	}
	/**
	 * Algorithm:
	 * <pre>
	 * CREATE TEMPORARY TABLE anon_map AS SELECT id FROM temp_patients WHERE FALSE;
	 * ALTER TABLE anon_map ADD COLUMN target(INTEGER NOT NULL AUTO_INCREMENT);
	 * INSERT INTO anon_map(id) (SELECT DISTINCT id FROM temp_patients);
	 * ALTER TABLE temp_patients ADD COLUMN a_id(INTEGER NOT NULL);
	 * UPDATE temp_patients SET a_id=map.target WHERE id=map.id (FROM anon_Map map);
	 * ALTER TABLE temp_patients DROP COLUMN id; 
	 * ... also for ref tables
	 * </pre>
	 * @param anonymize anonymization key with reference
	 * @throws SQLException SQL error
	 */
	private void doAnonymisation(AnonymizeKey anonymize) throws SQLException{
		// batch statements which need to be executed in order
		List<String> batch = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TEMPORARY TABLE anon_map AS SELECT ");
		sql.append(anonymize.key.column);
		sql.append(" AS id FROM ");
		sql.append(anonymize.key.table);
		sql.append(" WHERE FALSE");
		batch.add(sql.toString());

		// add auto increment column
		batch.add("ALTER TABLE anon_map ADD COLUMN target(INTEGER NOT NULL AUTO_INCREMENT)");

		// fill ids and generate new anonymized ids
		sql = new StringBuilder();
		sql.append("INSERT INTO anon_map(id) (SELECT DISTINCT ");
		sql.append(anonymize.key.column);
		sql.append(" FROM ");
		sql.append(anonymize.key.table);
		sql.append(")");
		batch.add(sql.toString());

		// next steps are also repeated for each reference table
		anonymizeReference(batch, anonymize.key);
		for( TableColumn ref : anonymize.ref ){
			anonymizeReference(batch, ref);
		}
		// execute the batch statements
		try{
			for( int i=0; i<batch.size(); i++ ){
				Statement s = dbc.createStatement();
				s.executeUpdate(batch.get(i));
				s.close();
				SQLWarning w = s.getWarnings();
				if( w != null ){
					handleWarning(batch.get(i), w);
				}
			}
		}catch( SQLException e ){
			// batch execution failed.
			// make sure to drop the temporary map in the end
			try( Statement s = dbc.createStatement() ){
				s.executeUpdate("DROP TEMPORARY TABLE anon_map IF EXISTS");
			}catch( SQLException e2 ){
				e.addSuppressed(e2);
			}
			throw e;
		}
	}
	@Override
	public void run() {
		Objects.requireNonNull(batch, "prepareStatements must be called prior to run");
		// do calculations
		try{
			for( Source s : query.source ){
				runStatements();
			}
			// anonymisation
			for( AnonymizeKey anon : query.anonymize ){
				doAnonymisation(anon);
			}
		}catch( SQLException e ){
			// failed
			cleanup();
			// TODO store error status somewhere
			return;
		}
		// export
		// TODO create folder and files
		try{
			for( ExportTable ex : query.export ){
				exportTable(ex, null);
			}
		}catch( IOException e ){
			e.printStackTrace();
		}catch( SQLException e ){
			e.printStackTrace();
		}
		cleanup();
	}
	
}
