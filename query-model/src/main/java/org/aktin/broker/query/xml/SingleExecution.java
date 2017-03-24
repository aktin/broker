package org.aktin.broker.query.xml;

import java.time.Instant;

import javax.xml.bind.annotation.XmlElement;

/**
 * Single execution schedule for a query.
 * 
 * A reference time stamp is provided.
 * 
 * @author R.W.Majeed
 *
 */
public class SingleExecution extends QuerySchedule{
	/**
	 * Reference time stamp for queries. If 
	 */
	@XmlElement(required=false)
	public Instant reference;
	
	
}
