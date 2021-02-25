package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.NotificationListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestProcessExecutionService {

	@Mock
	BrokerClient2 client;
	@Mock
	WebSocket websocket;


	private ProcessExecutionConfig loadConfig() throws IOException {
		try( InputStream in = getClass().getResourceAsStream("/sysproc.properties") ){
			return new ProcessExecutionConfig(in);
		}		
	}

	@Test
	public void verifyConfig() throws IOException {
		ProcessExecutionConfig config = loadConfig();
		Assertions.assertNotNull(config.getAuthClass());
		Assertions.assertNotNull(config.getAuthParam());
	}

	@Test
	public void startupAndShutdown() throws IOException, InterruptedException, ExecutionException {
		Assertions.assertNotNull(client);
		ArgumentCaptor<NotificationListener> captor = ArgumentCaptor.forClass(NotificationListener.class);

		when(client.openWebsocket(Mockito.any())).thenReturn(websocket);
		ProcessExecutionService service = new ProcessExecutionService(client, loadConfig());
		service.startupWebsocketListener();
		
		// make sure websocket was opened and we captured the notification listener
		verify(client, Mockito.times(1)).openWebsocket(captor.capture());
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
		
	}
}
