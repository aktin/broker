package org.aktin.broker.xml.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Util {
	public static String readContent(Reader reader) throws IOException{
		StringBuilder builder = new StringBuilder();
		int c = -1;
		char[] chars = new char[1024];
		do{
			c = reader.read(chars, 0, chars.length);
			//if we have valid chars, append them to end of string.
			if( c > 0 ){
				builder.append(chars, 0, c);
			}
		}while(c>0);
		return builder.toString();
	}
	public static Document parseDocument(Reader reader) throws IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			return factory.newDocumentBuilder().parse(new InputSource(reader));
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}
	}
	public static String formatHttpDate(Instant instant){
		// TODO java 7 incompatible
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
	}
	public static void printDOM(Node node, OutputStream out, String encoding) throws TransformerException{
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer;
			transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	    transformer.transform(new DOMSource(node), 
	         new StreamResult(out));
	}

}
