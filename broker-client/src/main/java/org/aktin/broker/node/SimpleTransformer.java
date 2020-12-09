package org.aktin.broker.node;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

public class SimpleTransformer {
	private Transformer transformer;

	public void loadTransformer(String path) throws TransformerException{
		TransformerFactory tf = TransformerFactory.newInstance();
		// disable external entity access
		tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		this.transformer = tf.newTransformer(new StreamSource(new File(path)));
	}

	public boolean hasTransformer(){
		return transformer != null;
	}
	public Document transform(Document source) throws TransformerException{
		DOMSource dom = new DOMSource(source);
		DocumentBuilder b;
		try {
			DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
			df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");	
			b = df.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new TransformerException("Failed to create document builder", e);
		}
		Document doc = b.newDocument();
		DOMResult res = new DOMResult(doc);
		transformer.transform(dom, res);
		return doc;
	}

}
