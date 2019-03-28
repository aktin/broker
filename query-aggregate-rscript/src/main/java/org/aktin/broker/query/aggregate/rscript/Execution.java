package org.aktin.broker.query.aggregate.rscript;

import java.util.logging.Logger;


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

	private RSource query;

	public Execution(RSource query){
		this.query = query;
	}
	
}
