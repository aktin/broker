package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.WebSocket;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.ClientNotificationListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TestProcessExecutionService {

	@Mock
	BrokerClient2 client;
	@Mock
	WebSocket websocket;


	private ProcessExecutionPlugin loadConfig() throws IOException {
		try( InputStream in = getClass().getResourceAsStream("/sysproc.properties") ){
			return new ProcessExecutionPlugin(in);
		}		
	}

	@Test
	public void verifyConfig() throws IOException {
		ProcessExecutionPlugin config = loadConfig();
		Assertions.assertNotNull(config.getAuthClass());
		Assertions.assertNotNull(config.getAuthParam());
		Assertions.assertNotNull(config.getRequestValidation());
	}

	//@Test
	public void startupAndShutdown() throws IOException, InterruptedException, ExecutionException {
		Assertions.assertNotNull(client);
		ArgumentCaptor<ClientNotificationListener> captor = ArgumentCaptor.forClass(ClientNotificationListener.class);

		when(client.connectWebsocket()).thenReturn(websocket);
		when(client.getWebsocket()).thenReturn(websocket);
		when(client.getMyRequestDefinition(eq(100), any(String.class), any(Path.class), ArgumentMatchers.<OpenOption>any())).thenReturn(Path.of("bla"));
		ProcessExecutionService service = new ProcessExecutionService(client, loadConfig());
		service.establishWebsocketConnection();
		
		// make sure websocket was opened and we captured the notification listener
		verify(client, Mockito.times(1)).addListener(captor.capture());
		// publish one request
		Assertions.assertEquals(1, captor.getAllValues().size());
		//captor.getValue().onRequestPublished(100);
		Future<ProcessExecution> f = service.addRequest(100);

		// wait for process
		ProcessExecution e = f.get();
		Assertions.assertFalse(e.isRunning());
		Assertions.assertFalse(e.isFailed());
		
		List<ProcessExecution> unprocessed = service.shutdown();
		verify(websocket, Mockito.times(1)).abort();
		Assertions.assertEquals(0, unprocessed.size());
		//Assertions.assertEquals(100, unprocessed.get(0).getRequestId());
		service.close();
		
	}
}
