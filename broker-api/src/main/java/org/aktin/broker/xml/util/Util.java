package org.aktin.broker.xml.util;

import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
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
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
	}
}
