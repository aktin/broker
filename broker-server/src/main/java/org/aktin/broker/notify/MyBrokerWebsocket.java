package org.aktin.broker.notify;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.aktin.broker.auth.Principal;


@ServerEndpoint(value="/broker/my/websocket", configurator=SessionConfigurator.class)
public class MyBrokerWebsocket {
	private static final Logger log = Logger.getLogger(MyBrokerWebsocket.class.getName());
	private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

	@OnOpen
	public void open(Session session){
		log.info("Session created: "+session.getId());
		clients.add(session);
		try {
			session.getBasicRemote().sendText("welcome "+session.getUserPrincipal());
		} catch (IOException e) {
			log.log(Level.WARNING,"Unable to send welcome message", e);
		}
		// TODO check privileges and close connection if invalid
	}
	@OnClose
	public void close(Session session){
		clients.remove(session);
		log.info("Session closed: "+session.getId());
	}
	@OnMessage
	public void message(Session session, String message){
		log.info("Session message: "+session.getId()+": "+message);
	}

	private static int broadcast(String message, boolean adminOnly){
		if( clients.isEmpty() ){
			return 0;
		}
		int count = 0;
		for( Session session : clients ){
			Principal user = (Principal)session.getUserProperties().get(SessionConfigurator.AUTH_USER);
			if( user == null ) {
				log.warning("Skipping websocket session without authentication "+session);
				continue;
			}
			if( adminOnly && !user.isAdmin() ) {
				// skip if not admin privileges
				continue;
			}
			if( session.isOpen() ){
				session.getAsyncRemote().sendText(message);
				count ++;
			}
		}
		return count;
	}

	
	public static void broadcastRequestPublished(int requestId){
		// transmitted to all clients and administrators
		broadcast("published "+requestId, false);
	}
	public static void broadcastRequestClosed(int requestId){
		// transmitted to all clients and administrators		
		broadcast("closed "+requestId, false);
	}
	public static void broadcastRequestNodeStatus(int requestId, int nodeId, String status){
		// transmitted only to administrators
		broadcast("status "+requestId+" "+nodeId+" "+status, true);	
	}
	// TODO method to retrieve connected client nodes (for more accurate information about last seen => active) 
}
