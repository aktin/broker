package org.aktin.broker.client2;

import java.io.IOException;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class WebsocketAdminListener implements WebSocket.Listener{
	private AdminNotificationListener listener;

	@Override
	public void onOpen(WebSocket webSocket) {
		Listener.super.onOpen(webSocket);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		String dataString = ""+data;
		String[] args = dataString.split(" ", 4);
		webSocket.request(1);
		if( last == true && args.length > 1 ) {
			switch( args[0] ) {
			case "created": // request created (requestId)
				listener.onRequestCreated(Integer.valueOf(args[1]));
				break;
			case "published": // request published (requestId)
				listener.onRequestPublished(Integer.valueOf(args[1]));
				break;
			case "closed": // request closed (requestId)
				listener.onRequestClosed(Integer.valueOf(args[1]));
				break;
			case "status": // request status reported by node (requestId, nodeId, status)
				listener.onRequestStatusUpdate(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3]);
				break;
			case "result": // request result uploaded by node (requestId, nodeId, mediaType)
				listener.onRequestResultUpdate(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3]);
				break;
			case "resource": // resource changed by node (nodeId, resourceId)
				listener.onResourceUpdate(Integer.valueOf(args[1]), args[2]);
				break;
			default:
				// ignoring unsupported websocket command
				// TODO log warning
			}
			return CompletableFuture.completedStage(null);
		}else {
			// all messages are short and there should be no partial messages
			return CompletableFuture.failedStage(new IOException("Partial websocket message not supported"));
		}
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		// TODO Auto-generated method stub
		return Listener.super.onClose(webSocket, statusCode, reason);
	}

}
