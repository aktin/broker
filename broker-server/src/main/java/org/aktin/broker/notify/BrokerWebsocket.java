package org.aktin.broker.notify;

import java.io.IOException;
import java.time.Instant;
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

@ServerEndpoint("/broker-notify")
public class BrokerWebsocket {
	private static final Logger log = Logger.getLogger(BrokerWebsocket.class.getName());
	private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

	@OnOpen
	public void open(Session session){
		log.info("Session created: "+session.getId());
		clients.add(session);
		try {
			session.getBasicRemote().sendText("welcome "+Instant.now().toString());
		} catch (IOException e) {
			log.log(Level.WARNING,"Unable to send welcome message", e);
		}
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

	public static int broadcast(String message){
		if( clients.isEmpty() ){
			return 0;
		}
		int count = 0;
		for( Session session : clients ){
			if( session.isOpen() ){
				try {
					session.getBasicRemote().sendText(message);
					count ++;
				} catch (IOException e) {
					log.log(Level.WARNING, "Unable to send to websocket "+session.getId(), e);
				}
			}
		}
		return count;
	}
}
