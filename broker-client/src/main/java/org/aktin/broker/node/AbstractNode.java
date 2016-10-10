package org.aktin.broker.node;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.ClientAuthenticator;
import org.aktin.broker.xml.SoftwareModule;
import org.w3c.dom.Document;

public abstract class AbstractNode {
	protected long startup;
	protected BrokerClient broker;
	private Transformer transformer;

	public AbstractNode(){
		this.startup = System.currentTimeMillis();
	}
	public final void connectBroker(String broker_endpoint, ClientAuthenticator auth) throws IOException{
		try {
			this.broker = new BrokerClient(new URI(broker_endpoint));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		broker.setClientAuthenticator(auth);
		// optional status exchange
		broker.getBrokerStatus();
		broker.postMyStatus(startup, 
				new SoftwareModule("org.aktin.broker.node", AbstractNode.class.getPackage().getImplementationVersion()),
				new SoftwareModule(getModuleName(), getModuleVersion())
		);
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

	public void loadTransformer(String path) throws TransformerException{
		TransformerFactory tf = TransformerFactory.newInstance();
		this.transformer = tf.newTransformer(new StreamSource(new File(path)));
	}

	public boolean hasTransformer(){
		return transformer != null;
	}
	public Document transform(Document source) throws TransformerException{
		DOMSource dom = new DOMSource(source);
		DocumentBuilder b;
		try {
			b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new TransformerException("Failed to create document builder", e);
		}
		Document doc = b.newDocument();
		DOMResult res = new DOMResult(doc);
		transformer.transform(dom, res);
		return doc;
	}

}
