package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.aktin.broker.client.auth.ClientAuthenticator;
import org.w3c.dom.Node;

public abstract class AbstractClient {
	private URI endpointURI;
	private URI aggregatorEndpoint;
	private ClientAuthenticator auth;
	
	public AbstractClient(URI endpointURI){
		setEndpoint(endpointURI);
	}
	public URI resolveBrokerURI(String spec){
		return endpointURI.resolve(spec);
	}
	public URI resolveAggregatorURI(String spec){
		return aggregatorEndpoint.resolve(spec);
	}
	public void setAggregatorEndpoint(URI uri){
		this.aggregatorEndpoint = uri;
	}
	/**
	 * Set the endpoint URI. This method will be called automatically 
	 * from {@link #AbstractClient(URI)}. If using a different constructor,
	 * make sure that the endpoint URI is set before any client method is
	 * called.
	 * 
	 * @param endpointURI endpoint URI
	 */
	public void setEndpoint(URI endpointURI){
		this.endpointURI = endpointURI;
	}

	public URI getEndpoint(){
		return this.endpointURI;
	}
	public void setClientAuthenticator(ClientAuthenticator clientAuth){
		this.auth = clientAuth;
	}
	protected final HttpURLConnection openConnection(String method, String spec) throws IOException{
		return openConnection(method, endpointURI.resolve(spec));
	}
	protected HttpURLConnection openConnection(String method, URI uri) throws IOException{
		URL url = uri.toURL();
		HttpURLConnection c;
		if( this.auth != null ){
			// use authentication
			c = auth.openAuthenticatedConnection(url);
		}else{
			// no authentication, use default connection
			c = (HttpURLConnection)url.openConnection();
		}
		c.setRequestMethod(method);
		return c;
	}	
	protected void delete(URI resource) throws IOException{
		HttpURLConnection c = openConnection("DELETE", resource);
		c.getInputStream().close();
	}
	protected static void writeContent(OutputStream dest, InputStream content) throws IOException{
		byte[] buf = new byte[2048];
		while( true ){
			int c = content.read(buf);
			if( c == -1 ){
				break;
			}
			dest.write(buf, 0, c);
		}
	}
	protected static void writeContent(Node node, OutputStream dest, String charset)throws IOException{
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		}
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputKeys.INDENT, "no");
	    transformer.setOutputProperty(OutputKeys.ENCODING, charset);
//	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	    try {
			transformer.transform(new DOMSource(node), new StreamResult(dest));
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}
	protected static void writeContent(String content, OutputStream out, String charset) throws IOException{
		OutputStreamWriter w = new OutputStreamWriter(out, charset);
		w.write(content);
		w.flush();
		// don't close the print writer, because this would also close the output stream
	}

}
