package org.aktin.broker.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.aktin.broker.auth.Principal;


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
	public static void broadcastRequestPublished(int requestId, int[] nodeIds){
		broadcastToSubset("published "+requestId, nodeIds);
	}
	public static void broadcastRequestClosed(int requestId, int[] nodeIds){
		broadcastToSubset("closed "+requestId, nodeIds);
	}

	public static void broadcastToNode(int nodeId, String message){
		// transmitted to all clients and administrators
		broadcast(clients, message, p -> p.getNodeId() == nodeId);
	}


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
