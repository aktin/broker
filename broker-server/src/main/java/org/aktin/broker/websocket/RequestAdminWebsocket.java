package org.aktin.broker.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.aktin.broker.auth.Principal;


@ServerEndpoint(value=RequestAdminWebsocket.REST_PATH, configurator = HeaderAuthSessionConfigurator.class)
public class RequestAdminWebsocket extends AbstractBroadcastWebsocket{
	public static final String REST_PATH = "/broker/request/websocket";
	/** set of connected sessions, needs to be static and local */ 
	private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

	private static final Logger log = Logger.getLogger(RequestAdminWebsocket.class.getName());
	
	public static void broadcastRequestPublished(int requestId){
		// transmitted to all clients and administrators
		broadcast(clients, "published "+requestId, false);
	}
	public static void broadcastRequestClosed(int requestId){
		// transmitted to all clients and administrators		
		broadcast(clients, "closed "+requestId, false);
	}
	public static void broadcastRequestNodeStatus(int requestId, int nodeId, String status){
		// transmitted only to administrators
		broadcast(clients, "status "+requestId+" "+nodeId+" "+status, true);	
	}
	// TODO method to retrieve connected client nodes (for more accurate information about last seen => active) 
	@Override
	protected boolean isAuthorized(Principal principal) {
		if( !principal.isAdmin() ) {
			log.log(Level.INFO, "Non admin client rejected for request admin websocket: {}", principal.getName());
			return false;
		}
		return true;
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
