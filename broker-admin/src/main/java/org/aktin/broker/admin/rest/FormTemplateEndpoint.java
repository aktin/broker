package org.aktin.broker.admin.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("template")
public class FormTemplateEndpoint {
	private static final Logger log = Logger.getLogger(FormTemplateEndpoint.class.getName());

	/**
	 * Retrieve list/details of available request templates.
	 * @return JSON string
	 * @throws IOException IO error
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String listTemplates() throws IOException {
		StringBuilder b = new StringBuilder();
		b.append("{\n");
		String templateRoot = "/webapp/template";
		URL templateUrl = getClass().getResource(templateRoot+"/index.txt");
		if( templateUrl == null ) {
			log.severe("Template resource folder not found");
			throw new NotFoundException();
		}
		try( InputStream in = templateUrl.openStream();
				BufferedReader r = new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
			String name;
			boolean firstTemplate = true;
			while( (name = r.readLine()) != null ) {
				if( name.trim().length() == 0 || name.startsWith("#") ) {
					// skip empty lines or comments
					continue;
				}
				Properties props = new Properties();
				URL purl = getClass().getResource(templateRoot+"/"+name+"/template.properties");
				try( InputStream pin = purl.openStream() ){
					if( pin == null ) {
						continue;
					}
					props.load(pin);
					// make sure commas are written correctly
					if( firstTemplate == false ) {
						b.append(",\n");
					}
					firstTemplate = false;
					
					b.append("\t\"").append(name).append("\": {");
					// write properties
					boolean firstProp = true;
					for( String key : props.stringPropertyNames() ) {
						if( firstProp == false ) {
							b.append(", ");
						}
						firstProp = false;
						b.append("\"").append(key).append("\": \"").append(props.getProperty(key)).append("\"");
					}
					b.append("}");
				}catch( IOException e ) {
					log.log(Level.WARNING, "Error loading form template properties", e);
				}
			}
		}
		b.append("\n}");
		return b.toString();
	}

	@GET
	@Path("script")
	public Response getScript(@QueryParam("mediaType") String mediaType) {
		// TODO send redirect to resource
		return null;
		
	}
	
}
