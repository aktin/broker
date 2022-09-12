package org.aktin.broker.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.aktin.broker.auth.Principal;

/**
 * Websocket endpoint to notify connected clients about updates for requests.
 *  
 * Notifications are only sent for requests associated with the client, e.g., 
 * which the client can access.
 * Once connected, the node/client will receive a notification when a new request 
 * was published or a currently published request was closed.
 * See {@link #broadcastRequestPublished(int, int[])} and {@link #broadcastRequestClosed(int, int[])}. 
 * 
 * Notifications have the form {@code published 123} or {@code closed 123} ({@code 123} being the request id).
 * The client does not send any data via websocket, except for optional ping-pong messages to probe the connection.
 * Notifications are only sent from server to client.
 *
 * @author R.W.Majeed
 *
 */
@ServerEndpoint(value=MyBrokerWebsocket.REST_PATH, configurator = HeaderAuthSessionConfigurator.class)
// jetty/jersey does not support CDI for the configurator. If using with jersey, initialize the configurator instance manually
public class MyBrokerWebsocket extends AbstractBroadcastWebsocket{
	public static final String REST_PATH = "/broker/my/websocket";
	private static final Logger log = Logger.getLogger(MyBrokerWebsocket.class.getName());
	/** set of connected sessions, needs to be static and local */ 
	private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

	private static Set<Integer> integerArraySet(int[] values){
		Set<Integer> nodes = new HashSet<Integer>(values.length);
		for( int i=0; i<values.length; i++ ) {
			nodes.add(values[i]);
		}
		return nodes;
	}
	private static void broadcastToSubset(String message, int[] nodeIds) {
		if( nodeIds == null ) {
			broadcast(clients, message);
		}else {
			final Set<Integer> nodes = integerArraySet(nodeIds);
			broadcast(clients, message, p -> nodes.contains(p.getNodeId()));
		}		
	}
	
	/**
	 * Notify the specified nodes that the given request was newly published.
	 * 
	 * @param requestId id of the newly published request
	 * @param nodeIds nodes to notify
	 */
	public static void broadcastRequestPublished(int requestId, int[] nodeIds){
		broadcastToSubset("published "+requestId, nodeIds);
	}

	/**
	 * Notify the specified nodes that the given request was just closed.
	 * 
	 * @param requestId id of the closed request
	 * @param nodeIds nodes to notify
	 */
	public static void broadcastRequestClosed(int requestId, int[] nodeIds){
		broadcastToSubset("closed "+requestId, nodeIds);
	}

//	private static void broadcastToNode(int nodeId, String message){
//		// transmitted to all clients and administrators
//		broadcast(clients, message, p -> p.getNodeId() == nodeId);
//	}


	@Override
	protected boolean isAuthorized(Principal principal) {
		// admin is not allowed to connect as node
		if( principal.isAdmin() ) {
			log.info("Admin role not allowed for node websocket connections");
			return false;
		}else {
			return true;
		}
	}




	@Override
	protected void addSession(Session session, Principal user) {
		user.incrementWebsocketCount();
		clients.add(session);
	}




	@Override
	protected void removeSession(Session session, Principal user) {
		user.decrementWebsocketCount();
		clients.remove(session);
	}
}
