package org.aktin.broker.client2;

import java.io.IOException;

import lombok.Getter;

public class MediaTypeNotAcceptableException extends IOException {

	private static final long serialVersionUID = 1L;

	public MediaTypeNotAcceptableException(String requestedType) {
		super("Requested media type not acceptable by broker: "+requestedType);
		this.requestedType = requestedType;
	}
	@Getter
	private String requestedType;

}
