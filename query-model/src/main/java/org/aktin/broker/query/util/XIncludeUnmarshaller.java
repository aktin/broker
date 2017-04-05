package org.aktin.broker.query.util;

import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XIncludeUnmarshaller {
	protected XMLReader reader;
	
	public XIncludeUnmarshaller() throws SAXException{
		SAXParser parser;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setXIncludeAware(true);
			factory.setNamespaceAware(true);
			// do not insert xml:base attributes for XIncludes
			factory.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
		reader = parser.getXMLReader();
	}

	public Source getResource(URL url){
		SAXSource xml = new SAXSource(reader, new InputSource(url.toString()));
		//xml.setSystemId(doc.toString());
		return xml;
	}

	public Source getResource(String name){
		return getResource(getClass().getResource(name));
	}
	public static final Source getXIncludeResource(String name){
		try {
			return new XIncludeUnmarshaller().getResource(name);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}
}
