package org.aktin.broker.xml;

import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.junit.Before;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class TestMarshallUnmarshall {
	protected XMLReader reader;
	
	@Before
	public void initializeXIncludeReader() throws SAXException{
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

}
