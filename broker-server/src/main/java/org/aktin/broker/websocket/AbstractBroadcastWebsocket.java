package org.aktin.broker.websocket;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.aktin.broker.auth.Principal;

import lombok.extern.java.Log;

@Log
public abstract class AbstractBroadcastWebsocket {

	protected abstract boolean isAuthorized(Principal principal);
	protected abstract void addSession(Session session, Principal user);
	protected abstract void removeSession(Session session, Principal user);


	@OnOpen
	public void open(Session session){
		Principal user = getSessionPrincipal(session);
		log.log(Level.INFO, "Websocket session {0} created for user {1}", new Object[] {session.getId(), user});

		// check privileges and close session if needed
		if( isAuthorized(user) ) {
			addSession(session, user);

		}else {
			// unauthorized, close session
			try {
				session.close();
				return;
			} catch (IOException e) {
				log.log(Level.WARNING,"Failed to close session", e);
			}
		}
		// send welcome message
		try {
			session.getBasicRemote().sendText("welcome "+user.getName());
		} catch (IOException e) {
			log.log(Level.WARNING,"Unable to send welcome message", e);
		}
	}
	@OnClose
	public void close(Session session){
		removeSession(session, getSessionPrincipal(session));
		log.info("Websocket session closed: "+session.getId());
	}

	@OnMessage
	public void message(Session session, String message){
		Principal user = getSessionPrincipal(session);
		log.log(Level.INFO, "Ignoring message from client {0}",user.getName());
	}
	@OnError
	public void error(Session session, Throwable t) {
	    log.log(Level.INFO, "Websocket error reported for session {0}: {1}", new Object[] {session.getId(), t});
	}

	static int broadcast(Set<Session> clients, String message){
		// if no filter is supplied, broadcast to all nodes
		return broadcast(clients, message, p -> true);
	}

	static int broadcast(Set<Session> clients, String message, Predicate<Principal> principalFilter){
		Objects.requireNonNull(principalFilter);
		if( clients.isEmpty() ){
			return 0;
		}
		int count = 0;
		// loop through connected clients
		for( Session session : clients ){
			Principal user = getSessionPrincipal(session);
			if( user == null ) {
				log.log(Level.WARNING,"Skipping websocket session {0} without authentication",session.getId());
				continue;
			}
			if( principalFilter.test(user) == false ) {
				// skip filtered
				continue;
			}
			if( session.isOpen() ){
				session.getAsyncRemote().sendText(message);
				count ++;
			}
		}
		return count;
	}

	/**
	 * Get authentication info for a given websocket session
	 * @param session session
	 * @return principal
	 */
	protected static Principal getSessionPrincipal(Session session) {
		return (Principal)session.getUserProperties().get(HeaderAuthSessionConfigurator.AUTH_USER);
	}

}
