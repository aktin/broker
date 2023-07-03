# Changelog

## v1.5.1
Silent connection drops now detectable via ping-pong message matching.
Fix websocket error "Websocket already connected" during ping-pong websocket reconnect.
Improved logging for websocket connection issues
Custom http proxy authenticator added org.aktin.broker.client2.auth.ApiKeyAuthenticationWithProxyPassword


## v1.5
REST admin endpoint added to delete submitted results from server
Polling only mode added for CLI client
Updated dependencies hsql and liquibase to most recent versions


## v1.4
Command line client with java plugin architecture. 
Docker containers will be built with JDK 17
