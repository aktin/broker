package org.aktin.broker.websocket;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.aktin.broker.auth.Principal;

public abstract class AbstractBroadcastWebsocket {
	private static final Logger log = Logger.getLogger(AbstractBroadcastWebsocket.class.getName());

	protected abstract boolean isAuthorized(Principal principal);
	protected abstract void addSession(Session session, Principal user);
	protected abstract void removeSession(Session session, Principal user);


	@OnOpen
	public void open(Session session){
		Principal user = getSessionPrincipal(session);
		log.log(Level.INFO, "Session id {} created for user {}", new Object[] {session.getId(), Objects.toString(user)});

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
		log.info("Session closed: "+session.getId());
	}

	@OnMessage
	public void message(Session session, String message){
		Principal user = getSessionPrincipal(session);
		log.log(Level.INFO, "Ignoring message from client {}",user.getName());
	}


	static int broadcast(Set<Session> clients, String message, boolean adminOnly){
		if( clients.isEmpty() ){
			return 0;
		}
		int count = 0;
		for( Session session : clients ){
			Principal user = getSessionPrincipal(session);
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

	/**
	 * Get authentication info for a given websocket session
	 * @param session session
	 * @return principal
	 */
	protected static Principal getSessionPrincipal(Session session) {
		return (Principal)session.getUserProperties().get(HeaderAuthSessionConfigurator.AUTH_USER);
	}

}
