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
	public String getElementName();
	public String getNamespace();
	public String formatTimestamp(Instant timestamp);
	public QueryHandler parse(Element element, Function<String,String> propertyLookup);
}
