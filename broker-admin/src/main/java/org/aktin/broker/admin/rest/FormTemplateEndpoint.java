package org.aktin.broker.admin.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("template")
public class FormTemplateEndpoint {
	private static final Logger log = Logger.getLogger(FormTemplateEndpoint.class.getName());

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String listTemplates() throws IOException {
		StringBuilder b = new StringBuilder();
		b.append("{\n");
		String templateRoot = "/webapp/template";
		try( InputStream in = getClass().getResourceAsStream(templateRoot);
				BufferedReader r = new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
			String name;
			boolean firstTemplate = true;
			while( (name = r.readLine()) != null ) {
				Properties props = new Properties();
				try( InputStream pin = getClass().getResourceAsStream(templateRoot+"/"+name+"/template.properties") ){
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
}
