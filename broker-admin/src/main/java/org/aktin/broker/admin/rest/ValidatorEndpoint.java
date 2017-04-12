package org.aktin.broker.admin.rest;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.query.xml.QueryRequest;

@Path("validator")
public class ValidatorEndpoint {
	private static final Logger log = Logger.getLogger(ValidatorEndpoint.class.getName());

	@POST
	@Path("check")
	@Consumes(MediaType.APPLICATION_XML)
	public void checkSyntax(QueryRequest request){
		log.info("Validation: "+request);
		System.out.println("validation?! "+request);
		// TODO validate SQL query extension syntax
	}
}
