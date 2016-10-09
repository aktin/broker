package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXB;

import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;

public abstract class AbstractBrokerClient extends AbstractClient{

	public AbstractBrokerClient(URI endpointURI) {
		super(endpointURI);
	}

	protected abstract URI getQueryBaseURI();
	
	public URI getQueryURI(String id){
		return getQueryBaseURI().resolve(id);
	}
	public String getQueryId(URI uri){
		return getQueryBaseURI().relativize(uri).getPath();
	}

	/**
	 * Make sure that a valid list is returned. TODO better solution
	 * @param list
	 * @return list
	 * @throws IOException error
	 */
	protected List<RequestInfo> postprocessRequestList(RequestList list) throws IOException{
		if( list == null ){
			throw new IOException("Unmarshalling of request list failed");
		}
		if( list.getRequests() != null ){
			return list.getRequests();
		}else{
			return Collections.emptyList();
		}		
	}
	/**
	 * Retrieve status information about the broker.
	 * @return broker status information
	 * @throws IOException io error
	 */
	public BrokerStatus getBrokerStatus() throws IOException{
		HttpURLConnection c = openConnection("GET", "status");
		try( InputStream response = c.getInputStream() ){
			return JAXB.unmarshal(response, BrokerStatus.class);
		}
	}
	protected Reader contentReader(HttpURLConnection c, String mediaType) throws UnsupportedEncodingException, IOException{
		c.addRequestProperty("Accept", mediaType);
		String contentType = c.getContentType();
		// use charset from content-type header
		int csi = contentType.indexOf("charset=");
		// default HTTP charset
		String charset = "ISO-8859-1";
		if( csi != -1 ){
			charset = contentType.substring(csi+8);
		}
		return new InputStreamReader(c.getInputStream(), charset);
	}

}