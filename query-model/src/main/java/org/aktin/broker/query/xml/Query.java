package org.aktin.broker.query.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;


/**
 * Query which can be executed in a data warehouse. The actual query definition(s)
 * are specified via {@link #extensions}.
 * 
 * @author R.W.Majeed
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Query {

	/**
	 * Unique identifier for this query. This identifier
	 * will not change, even if a query scheduled for repeated
	 * executions.
	 */
	@XmlElement(required=true)
	public int id;
	
	/**
	 * Human readable short description of the query.
	 */
	@XmlElement(required=true)
	public String title;
	/**
	 * Human readable description of the query.
	 */
	@XmlElement(required=true)
	public String description;
	
	/**
	 * Contact for inquiries and further
	 * information about the query and research
	 * project.
	 */
	@XmlElement(required=true)
	public Principal principal;

	/**
	 * Execution schedule for the query. For now,
	 * only {@link SingleExecution} and {@link RepeatedExecution} 
	 * are supported.
	 */
	@XmlElement(required=true)
	public QuerySchedule schedule;
	
	/**
	 * Extension to specify query definitions as well as result/export definitions.
	 * <p>
	 * E.g. for native i2b2: elements {@code query_definition} and {@code result_output_list}
	 * from XML namespace {@code http://www.i2b2.org/xsd/cell/crc/psm/1.1/}.
	 * </p>
	 */
	@XmlAnyElement
	public List<Element> extensions;

}
