package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import lombok.Data;
import lombok.Getter;

@Getter
public class ProcessExecutionConfig extends AbstractClientConfiguration{

	// specific config
	private List<String> command;
	private Path processLogDir;

	
	public ProcessExecutionConfig(InputStream in) throws IOException {
		super(in);
	}
	
	@Override
	protected void loadConfig(Properties props) {
		
		String cmd = props.getProperty("process.command");
		command = new ArrayList<>();
		command.add(cmd);
		command.addAll(Arrays.asList(props.getProperty("process.args").split("\\s+")));
		
		String logDir = props.getProperty("process.log.directory");
		if( logDir == null ) {
			this.processLogDir = null;
		}else {
			this.processLogDir = Paths.get(logDir);
		}
		// TODO allow configuration to force logging (via JUL) of request and response content types. e.g. property process.log.literal.mediatypes
	}
}
