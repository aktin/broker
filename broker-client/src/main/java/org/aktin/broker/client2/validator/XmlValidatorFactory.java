package org.aktin.broker.client2.validator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlValidatorFactory implements RequestValidatorFactory, RequestValidator{

	private DocumentBuilderFactory fac;
	private Schema schema;
	private String mediaType;

	/**
	 * The default constructor will produce validator instances checking for any XML.
	 * For specific XSD validation, use other constructor.
	 */
	public XmlValidatorFactory() {
		schema = null;
		mediaType = null;
		fac = DocumentBuilderFactory.newInstance();
		//fac.setAttribute(xml.XMLConstants.ACCESS_EXTERNAL_DTD, false);
		//fac.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, false);
		//fac.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, false);
		// TODO later version could support loading specific XSD to check against
	}
	
	public XmlValidatorFactory(Path schema) throws SAXException, IOException {
		loadSchema(schema);
	}

	@Override
	public RequestValidator getValidator(String mediaType) throws IllegalArgumentException {
		if( this.mediaType != null && !mediaType.startsWith(this.mediaType) ) {
			throw new IllegalArgumentException("Unsupported media type "+mediaType+". Validation only supported for "+this.mediaType);
		}else {
			// TODO check for XML media types, e.g. */*+xml[;$]
		}
		return this;
	}

	private void loadSchema(Path schemaPath) throws SAXException, IOException {
	    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    try( InputStream in = Files.newInputStream(schemaPath)){
	    	Source schemaFile = new StreamSource();
		    this.schema = factory.newSchema(schemaFile);
	    }
	}

	@Override
	public void validate(InputStream in) throws IOException, ValidationError {

	    // parse to DOM
	    DocumentBuilder builder;
		Document dom;
		try {
			builder = fac.newDocumentBuilder();
			dom = builder.parse(in);
		} catch (ParserConfigurationException e) {
			// unable to initialize builder
			throw new IOException(e);
		} catch (SAXException e) {
			// parsing failed
			throw new ValidationError(e);
		}
		// TODO check that document is not empty/ validate by schema
		if( schema == null ) {
			// no schema/XSD to check against.
			// if we are here, then the source document was parsed into DOM and is valid XML.
			// all good.
			return;
		}

	    Validator validator =  schema.newValidator();
		try {
			validator.validate(new DOMSource(dom));
		} catch (SAXException e) {
			throw new ValidationError(e);
		}
		
	}

}
