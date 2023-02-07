package org.aktin.broker.client.live.sysproc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.MediaTypeNotAcceptableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestProcessExecution {

	@Mock
	ProcessExecutionConfig config;

	@Mock
	BrokerClient2 client;

	private static String bashExecutable;
	private static Map<String,Path> scriptPaths;
	private static final String[] SCRIPT_NAMES = {"echo1.sh", "sleep.sh", "cat.sh"};
	

	@BeforeAll
	public static void verifyTestScripts() throws URISyntaxException {
		scriptPaths = new HashMap<>();
		for( String name : SCRIPT_NAMES ) {
			Path script = Paths.get(TestProcessExecution.class.getResource("/"+name).toURI());
			Assertions.assertTrue(Files.isReadable(script));
			scriptPaths.put(name, script);
		}
	}

	@BeforeAll
	public static void setupBash() {
		if( OS.WINDOWS.isCurrentOs() ) {
			findBashInWindows();
		}else if( OS.LINUX.isCurrentOs() ) {
			findBashInLinux();
		}
	}
	private static void findBashInWindows() {
		// try in windows/system32
		String windir = System.getenv("ProgramFiles");
		Path path = Paths.get(windir, "Git", "bin", "bash.exe");
		if( Files.isExecutable(path) ) {
			bashExecutable = path.toString();
			return;
		}
		String local = System.getenv("LOCALAPPDATA");
		// TODO find user specific installation
		Assertions.assertNotNull(local);
		Assertions.fail("bash not found on Windows system. Try installing git for windows which includes git-bash!");
	}
	
	private static void findBashInLinux() {
		String[] locations = new String[] {"/usr/bin/bash","/bin/bash"};
		for( String loc : locations ) {
			Path path = Paths.get(loc);
			if( Files.isExecutable(path) ) {
				bashExecutable = path.toString();
				return;
			}
		}
		
		Assertions.fail("bash not found on Linux system. Try installing bash!");
	}

	
	@Test
	public void expectProcessBuilderToExecuteScriptSuccessfully() throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList(bashExecutable,scriptPaths.get("echo1.sh").toString()));
		int exitCode = pb.start().waitFor();
		Assertions.assertEquals(0, exitCode);
	}

	@Test
	public void expectProcessTimeout() throws URISyntaxException, MediaTypeNotAcceptableException, IOException {
		Mockito.when(config.getCommand()).thenReturn(Arrays.asList(bashExecutable,scriptPaths.get("sleep.sh").toString(), "10"));
		Mockito.when(config.getProcessTimeoutMillis()).thenReturn(500L);
		Mockito.when(config.getRequestMediatype()).thenReturn("bla/bla");
		when(client.getMyRequestDefinition(eq(1), any(String.class), any(Path.class), ArgumentMatchers.<OpenOption>any())).thenReturn(Path.of("bla"));
		
		ProcessExecution exec = new ProcessExecution(1, config);
		exec.setClient(client);

		
		exec.run();
		System.out.println("Exception: "+exec.getCause());
		Assertions.assertEquals(TimeoutException.class, exec.getCause().getClass());
	}

	
	@Test
	public void expectProcessInteruptable() throws URISyntaxException, InterruptedException, MediaTypeNotAcceptableException, IOException {
		Mockito.when(config.getCommand()).thenReturn(Arrays.asList(bashExecutable,scriptPaths.get("sleep.sh").toString(), "10"));
		Mockito.when(config.getProcessTimeoutMillis()).thenReturn(10000L);
		Mockito.when(config.getRequestMediatype()).thenReturn("bla/bla");
		when(client.getMyRequestDefinition(eq(1), any(String.class), any(Path.class), ArgumentMatchers.<OpenOption>any())).thenReturn(Path.of("bla"));
		
		ProcessExecution exec = new ProcessExecution(1, config);
		exec.setClient(client);
		// start concurrent thread
		Thread thread = new Thread(exec);
		thread.start();
		// wait for the other thread to start working
		Thread.sleep(500L);
		Assertions.assertTrue(exec.isRunning());

		// try to abort the process
		exec.abortLocally();
		thread.interrupt();
		
		// wait for the thread to exit the aborted process
		Thread.sleep(500L);
		Assertions.assertFalse(exec.isRunning());
		Assertions.assertTrue(exec.isFailed());
		Assertions.assertEquals(CancellationException.class, exec.getCause().getClass());
	}


	@Test
	public void expectStdinAndStdoutWorking() throws URISyntaxException, InterruptedException, IOException {
		Random rand = new Random();
		String reqType = "text/plain+test-request-"+rand.nextInt();
		String resultType = "text/plain+test-result-"+rand.nextInt();
		String content = "a\nb\nrequest definition "+rand.nextInt();
		int requestId = rand.nextInt();
	
		Mockito.when(config.getCommand()).thenReturn(Arrays.asList(bashExecutable,scriptPaths.get("cat.sh").toString()));
		Mockito.when(config.getRequestMediatype()).thenReturn(reqType);
		Mockito.when(config.getResultMediatype()).thenReturn(resultType);
		Mockito.when(config.getProcessTimeoutMillis()).thenReturn(10000L);

		Mockito.when(client.getMyRequestDefinition(Mockito.eq(requestId), Mockito.eq(reqType), Mockito.any(Path.class), Mockito.any())).thenAnswer( (a) -> {
			Path dest = a.getArgument(2,Path.class);
			OpenOption[] args = new OpenOption[a.getArguments().length-3];
			for( int i=0; i< args.length; i++ ) {
				args[i] = a.getArgument(3+i, OpenOption.class);
			}
			// write content to path
			Files.writeString(dest, content, args);
			return dest;
		});
	
		
		ProcessExecution exec = new ProcessExecution(requestId, config);
		exec.setKeepTempFiles(true);
		exec.setClient(client);
		exec.run();
		
		Mockito.verify(client, Mockito.times(1)).putRequestResult(Mockito.eq(requestId), Mockito.eq(resultType), Mockito.any(InputStream.class));
		Assertions.assertFalse(exec.isRunning());
		Assertions.assertFalse(exec.isFailed());
		Assertions.assertNull(exec.getCause());
		// make sure output is same as input
		
		Assertions.assertEquals(content, Files.readString(exec.getResultPath(), StandardCharsets.UTF_8));
		// clean up files
		exec.cleanTempFiles();
	}

}
