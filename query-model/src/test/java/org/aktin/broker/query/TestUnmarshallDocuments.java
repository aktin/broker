package org.aktin.broker.query;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.bind.JAXB;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.aktin.broker.query.Query;
import org.aktin.broker.query.QueryRequest;
import org.aktin.broker.query.SingleExecution;
import org.aktin.broker.xml.TestMarshallUnmarshall;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class TestUnmarshallDocuments extends TestMarshallUnmarshall{
	Validator validator;
	
	@Before
	public void initializeValidator() throws IOException, SAXException{
		URL xsd = getClass().getResource("/schemagen/schema1.xsd");
        SchemaFactory factory = 
	            SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try( InputStream in = xsd.openStream() ){
        	schema = factory.newSchema(new StreamSource(in));
        }
        
	    validator = schema.newValidator();		
	}
	
	@Test
	public void validateQuery() throws IOException, SAXException{
		validator.validate(getResource("/query.xml"));
	}
	@Test
	public void unmarshallQuery() throws IOException, SAXException{
		Source xml = getResource("/query.xml");
		Query query = JAXB.unmarshal(xml, Query.class);
		Assert.assertEquals(SingleExecution.class, query.schedule.getClass());
		SingleExecution se = (SingleExecution)query.schedule;
		Assert.assertNotNull(se.duration);
		Assert.assertNotNull(se.reference);
		//System.out.println("Duration:"+se.duration);
		//System.out.println("Reference:"+se.reference);
		
		for( Element el : query.extensions ){
			System.out.println("Extension name="+el.getLocalName()+", ns="+el.getNamespaceURI());
		}		
	}
	@Test
	public void validateQueryRequest() throws IOException, SAXException, TransformerException{
		validator.validate(getResource("/request.xml"));

	}
	
	@Test
	public void unmarshallRequest(){
		QueryRequest r = JAXB.unmarshal(getResource("/request.xml"), QueryRequest.class);
		Assert.assertNotNull(r.published);
		Assert.assertNotNull(r.deadline);
	}

}
