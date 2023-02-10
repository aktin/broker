package org.aktin.broker.client.live.noop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.aktin.broker.client.live.AbortableRequestExecution;
import org.aktin.broker.client2.validator.RequestValidatorFactory;
import org.aktin.broker.client2.validator.ValidationError;

import lombok.extern.java.Log;

@Log
public class NoopExecution extends AbortableRequestExecution {
	private NoopExecutionPlugin config;
	private String requestBody;

	public NoopExecution(int requestId, NoopExecutionPlugin config) {
		super(requestId);
		this.config = config;
	}

	@Override
	protected void prepareExecution() throws IOException {
		
		HttpResponse<String> def = client.getMyRequestDefinition(requestId, config.getRequestMediatype(), BodyHandlers.ofString());
		String body = def.body();

		// validate if necessary.
		RequestValidatorFactory validator = config.getRequestValidator();
		if( validator != null ) {
			try {
				validator.getValidator(config.getRequestMediatype()).validate(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
			} catch (ValidationError e) {
				throw new IOException("Invalid request syntax", e);
			}
		}
		
		this.requestBody = body;
	}

	@Override
	protected void doExecution() throws IOException {
		// No operation / nothing happening in this implementation.
		// Usually, the execution logic would be performed here.
		log.info("NOOP execution of request "+getRequestId()+": "+requestBody);
	}

	@Override
	protected void finishExecution() {
		reportCompleted(); // will upload the result	
	}

	@Override
	protected String getResultMediatype() {
		return "text/plain";
	}

	@Override
	protected InputStream getResultData() {
		// we don't have any real result. just return dummy content
		return new ByteArrayInputStream("NOOP result".getBytes(StandardCharsets.UTF_8));
	}

}
