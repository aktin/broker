package org.aktin.broker.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aktin.broker.xml.RequestInfo;

/**
 * Broker node which executes the incoming requests synchronously
 * in the order of retrieval. Requests are not stored locally
 * and there is no human interaction.
 *
 * @author R.W.Majeed
 *
 */
public abstract class SynchronousNode extends AbstractNode {
	protected List<RequestProcessor> processors;
	protected List<RequestInfo> requests;

	public SynchronousNode(){
//		this.startup = System.currentTimeMillis();
		processors = new ArrayList<>();
	}

	public void addProcessor(RequestProcessor processor){
		this.processors.add(processor);
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

}
