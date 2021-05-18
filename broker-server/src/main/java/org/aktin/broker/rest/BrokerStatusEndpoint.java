package org.aktin.broker.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.SoftwareModule;

/**
 * Broker status service.
 * 
 * @author R.W.Majeed
 *
 */
@Path("/broker/status")
public class BrokerStatusEndpoint {
	public static final SoftwareModule BROKER_SERVER = new SoftwareModule("broker-server", BrokerStatusEndpoint.class.getPackage().getImplementationVersion());
	/**
	 * Retrieve status information about the broker.
	 * @return JSON status
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public BrokerStatus status(){
		BrokerStatus status = BrokerStatus.create();
		status.getSoftware().add(BROKER_SERVER);
		return status;
	}
}
