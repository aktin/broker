package org.aktin.broker.node;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.ClientAuthenticator;
import org.aktin.broker.xml.RequestInfo;

/** Abstract node supporting a single broker
 * 
 * @author R.W.Majeed
 *
 */
public abstract class AbstractNode {
	protected long startup;
	protected BrokerClient broker;
	protected List<RequestProcessor> processors;
	protected List<RequestInfo> requests;

	public AbstractNode(){
		this.startup = System.currentTimeMillis();
		processors = new ArrayList<>();
	}

	public void addProcessor(RequestProcessor processor){
		this.processors.add(processor);
	}
	/**
	 * Connect to the broker and exchange status information. After retrieving the remote
	 * status, the client status with modules and software versions are submitted.
	 * <p>
	 * To report additional software versions, override {@link #fillSoftwareModulesVersions(Map)}.
	 * </p>
	 * @param broker_endpoint endpoint URL
	 * @param auth authenticator
	 * @throws IOException IO error
	 */
	public final void connectBroker(String broker_endpoint, ClientAuthenticator auth) throws IOException{
		try {
			this.broker = new BrokerClient(new URI(broker_endpoint));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		broker.setClientAuthenticator(auth);
		// optional status exchange
		// retrieve server status
		broker.getBrokerStatus();
		// submit node status with software module versions
		Map<String,String> versions = new LinkedHashMap<>();
		// more modules
		fillSoftwareModulesVersions(versions);
		broker.postSoftwareVersions(versions);
	}

	protected void issueWarning(String warning){
		System.err.println("WARNING: "+warning);
	}

	/**
	 * Fill software modules. Override this method to add additional versions.
	 * The default implementation adds the JRE version, broker API version and
	 * the version of the implementing class.
	 *
	 * @param versions destination string map to store the versions
	 */
	protected void fillSoftwareModulesVersions(Map<String,String> versions){
		versions.put("java", System.getProperty("java.vendor")+"/"+System.getProperty("java.version"));
		versions.put("org.aktin.broker.node", AbstractNode.class.getPackage().getImplementationVersion());
		versions.put(getModuleName(), getModuleVersion());
	}
	/**
	 * Override this method to specify a name for your module.
	 * Default implementation uses {@code getClass().getName()}.
	 * @return module name
	 */
	public String getModuleName(){
		return getClass().getName();
	}
	/**
	 * Override this method to specify a module version number.
	 * @return version number
	 */
	public String getModuleVersion(){
		return getClass().getPackage().getImplementationVersion();
	}


}
