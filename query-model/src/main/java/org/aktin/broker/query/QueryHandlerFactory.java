package org.aktin.broker.query;

import java.time.Instant;
import java.util.function.Function;

import org.w3c.dom.Element;

/**
 * Factory to parse and process queries.
 * See {@link QueryHandler}.
 *
 * @author R.W.Majeed
 *
 */
public interface QueryHandlerFactory {
	/**
	 * Get the XML element name expected by this handler factory
	 * @return XML DOM element name
	 */
	public String getElementName();
	/**
	 * Get the XML element namespace expected by this handler factory
	 * @return XML DOM element namespace
	 */
	public String getNamespace();
	/**
	 * Format a timestamp for this query handler. This method is called to fill
	 * timestamps into the properties passed to the propertyLookup function in {@link #parse(Element, Function)}.
	 * @param timestamp timestamp to format
	 * @return formatted string
	 */
	public String formatTimestamp(Instant timestamp);

	/**
	 * Parse a query handler/processing stage from DOM element. When called, the {@code element} argument will
	 * match to the values from {@link #getElementName()} and {@link #getNamespace()}.
	 * 
	 * @param element DOM element containing the query
	 * @param propertyLookup function to be used to look up additional context information
	 *   about the environment of the query. e.g. time stamps, version numbers, etc.
	 *   The lookupFuntion can be used after exiting this function and is valid during 
	 *   the whole lifetime of the generated {@link QueryHandler}.
	 * @return new query handler
	 */
	public QueryHandler parse(Element element, Function<String,String> propertyLookup);
}
