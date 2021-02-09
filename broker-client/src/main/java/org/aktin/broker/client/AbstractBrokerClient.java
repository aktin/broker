package org.aktin.broker.client;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;

public abstract class AbstractBrokerClient extends AbstractClient{

	public AbstractBrokerClient(URI endpointURI) {
		super(endpointURI);
	}

	protected abstract URI getQueryBaseURI();
	
	public URI getQueryURI(int id){
		return getQueryBaseURI().resolve(Integer.toString(id));
	}
	public int getQueryId(URI uri){
		return Integer.parseInt(getQueryBaseURI().relativize(uri).getPath());
	}

	// Make sure that a valid list is returned. TODO better solution
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
	 * Retrieve reader to receive the content
	 * @param c URL connection
	 * @param mediaType media type, which will be specified in the {@code Accept} request header. If {@code null} this header is omitted.
	 * @return reader or {@code null} for status 404 not found
	 * @throws UnsupportedEncodingException if the response content type specified an unsupported encoding
	 * @throws IOException io error
	 */
	protected Reader contentReader(HttpURLConnection c, String mediaType) throws UnsupportedEncodingException, IOException{
		if( mediaType != null ){
			c.addRequestProperty("Accept", mediaType);
		}
		if( c.getResponseCode() == 404 ){
			// resource not found
			return null;
		}else if( c.getResponseCode() == 406 ){
			// unable to supply the requested media type
			// TODO allow user to notice/differentiate between 404 and 406 responses
			return null;
		}
		return Utils.contentReaderForInputStream(c.getInputStream(), c.getContentType(), StandardCharsets.ISO_8859_1);
	}

}
