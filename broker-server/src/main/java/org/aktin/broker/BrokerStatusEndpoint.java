package org.aktin.broker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.xml.BrokerStatus;

/**
 * Broker service.
 * 
 * TODO allow JSON media types (via JAXB mapping e.g. with MOXY)
 * TODO add RequestTypeManager
 * @author R.W.Majeed
 *
 */
@Path("/broker/status")
public class BrokerStatusEndpoint {

	/**
	 * Retrieve status information about the broker.
	 * @return JSON status
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public BrokerStatus status(){
		return BrokerStatus.create();
	}
}
