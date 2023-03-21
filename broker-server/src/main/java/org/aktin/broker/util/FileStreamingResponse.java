package org.aktin.broker.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FileStreamingResponse implements StreamingOutput{
	private Path file;

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		Files.copy(file, output);
	}

}
