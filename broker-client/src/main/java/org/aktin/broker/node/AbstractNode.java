package org.aktin.broker.node;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
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
		broker.postMyStatus(startup, versions);
	}

	protected void issueWarning(String warning){
		System.err.println("WARNING: "+warning);
	}
	/**
	 * Load requests from the broker and preprocess
	 * the list so only processable requests are remaining.
	 * @throws IOException IO error
	 */
	public void loadRequests() throws IOException{
		requests = broker.listMyRequests();
		// remove unprocessable requests from list
		Iterator<RequestInfo> iter = requests.iterator();
		while( iter.hasNext() ){
			RequestInfo request = iter.next();
			
			int i;
			for( i=0; i<processors.size(); i++ ){
				if( processors.get(i).canHandleRequest(request) ){
					break;
				}
			}
			if( i == processors.size() ){
				// no processor found to handle the request 
				// unsupported query type
				// need query definition and output list 
				iter.remove();
				issueWarning("Unsupported media types for query "+request.getId());
			}
		}
	}
	/**
	 * Whether or not we have requests to process.
	 * @return {@code true} if there are processable requests in the queue, {@code false} otherwise.
	 */
	public boolean hasRequests(){
		return !requests.isEmpty();
	}

	/**
	 * Process pending requests.
	 * @throws IOException IO error
	 */
	public void processRequests() throws IOException{
		// process requests for each processor
		for( RequestProcessor processor : processors ){
			List<RequestInfo> matches = new ArrayList<>();
			// process requests synchronously. first come first serve (for same processor)
			for( RequestInfo request : requests ){
				if( processor.canHandleRequest(request) ){
					matches.add(request);
				}
			}
			requests.removeAll(matches);
			processor.processRequests(matches, broker);
		}
		// there should be no remaining requests
		if( !requests.isEmpty() ){
			throw new AssertionError();
		}
	}

	/**
	 * Fill software modules. Override this method to add additional versions.
	 * @param modules destination string map to store the versions
	 */
	protected void fillSoftwareModulesVersions(Map<String,String> versions){
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
