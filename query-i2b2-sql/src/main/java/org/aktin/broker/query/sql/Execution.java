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

import org.aktin.broker.query.io.table.TableExport;
import org.aktin.broker.query.io.table.TableWriter;

/**
 * SQL export execution.
 * The export is performed in five steps:
 * <ol>
 * 	<li>Prepare SQL</li>
 *  <li>Create tables via {@link #generateTables(Connection)}. 
 *      This step also performs anonymization.</li>
 *  <li>Export tables</li>
 *  <li>Remove tables</li>
 * </ol>
 * @author R.W.Majeed
 *
 */
public class Execution{
	private static final Logger log = Logger.getLogger(Execution.class.getName());

	private Connection dbc;
	private SQLQuery query;
	private List<String> batch;
	private boolean failed;

	public Execution(SQLQuery query){
		this.query = query;
	}

	public void setConnection(Connection dbc){
		this.dbc = dbc;
	}

	public boolean isFailed(){
		return failed;
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
		log.warning("in ["+sql+"]:");
		log.warning("SQL error: "+e.getMessage());
	}

	/**
	 * Map for property substitution in SQL statements
	 * @param propertyLookup lookup table
	 * @throws SubstitutionError Thrown when properties could not be substituted because the propertyLookup did not supply some values
	 */
	public void prepareStatements(Function<String,String> propertyLookup) throws SubstitutionError{
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
			}catch( SQLException e ){
				handleException(sql, e);
				failed = true;
				throw e;
			}
			if( warning != null ){
				handleWarning(sql, warning);
			}
		}
	}

	public void removeTables(){
		log.info("Cleanup temporary tables");
		for( TemporaryTable table : query.tables){
			// XXX drop TEMPORARY not supported, make sure not to drop regular tables
			String sql = "DROP TABLE IF EXISTS "+table.name;
			log.info(sql);
			try( Statement s = dbc.createStatement() ){
				s.executeUpdate(sql);
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
	
	private static void anonymizeReference(List<String> batch, TableColumn reference){
		// add column to original table
		StringBuilder sql = new StringBuilder();
		sql.append("ALTER TABLE ").append(reference.table);
		sql.append(" ADD COLUMN a_").append(reference.column);
		sql.append(" INTEGER NULL");
		batch.add(sql.toString());
		// fill column in original table
		sql = new StringBuilder();
		sql.append("UPDATE ").append(reference.table);
		sql.append(" SET a_").append(reference.column);
		sql.append(" = map.target ");
		sql.append(" FROM anon_map map WHERE ");
		sql.append(reference.table).append('.').append(reference.column).append(" = map.id");
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
		batch.add("ALTER TABLE anon_map ADD COLUMN target SERIAL NOT NULL");

		// fill ids and generate new anonymized ids
		sql = new StringBuilder();
		sql.append("INSERT INTO anon_map SELECT DISTINCT ");
		sql.append(anonymize.key.column);
		sql.append(" AS id FROM ");
		sql.append(anonymize.key.table);
		sql.append("");
		batch.add(sql.toString());

		// next steps are also repeated for each reference table
		anonymizeReference(batch, anonymize.key);
		// anonymisation without references are possible,
		if( anonymize.ref != null ){ // process references only if provided
			for( TableColumn ref : anonymize.ref ){
				anonymizeReference(batch, ref);
			}
		}
		// drop anonymisation map before next anonymisation
		batch.add("DROP TABLE anon_map");
		// execute the batch statements
		try{
			for( int i=0; i<batch.size(); i++ ){
				Statement s = dbc.createStatement();
				log.info("Executing ["+batch.get(i)+"]");
				s.executeUpdate(batch.get(i));
				SQLWarning w = s.getWarnings();
				if( w != null ){
					handleWarning(batch.get(i), w);
				}
				s.close();
			}
		}catch( SQLException e ){
			// batch execution failed.
			// make sure to drop the temporary map in the end
			try( Statement s = dbc.createStatement() ){
				// DROP TEMPORARY not supported, XXX make sure no regular tables are dropped
				s.executeUpdate("DROP TABLE IF EXISTS anon_map");
			}catch( SQLException e2 ){
				e.addSuppressed(e2);
			}
			throw e;
		}
	}

	public void generateTables(Connection dbc) throws SQLException{
		Objects.requireNonNull(batch, "prepareStatements must be called prior to run");
		this.dbc = dbc;
		// do calculations
		runStatements();
		// anonymisation
		// query.anonymize can be null if no <anonymize> element is present
		if( query.anonymize != null ){
			for( AnonymizeKey anon : query.anonymize ){
				doAnonymisation(anon);
			}
		}
	}
	/**
	 * Export the generated tables.
	 * This method does not close the {@link TableExport}
	 * @param export export
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 */
	public void exportTables(TableExport export) throws SQLException, IOException{
		for( ExportTable ex : query.export ){
			String dest = ex.destination;
			if( dest == null || dest.length() == 0 ){
				dest = ex.table;
			}
			exportTable(ex, export.exportTable(dest));
		}
	}
	
}
