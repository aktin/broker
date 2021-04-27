# Configuration options

## Websocket idle timeout

Via system properties, you can set the websocket idle timout. This timeout
will cause idle websocket connections from clients to be closed after the 
specified amount of seconds. Use the system variable `broker.websocket.idletimeoutseconds`

## Authentication providers

Different authentication providers can be configured by setting the system
property `broker.auth.provider` to a comma separated list of classes, which
implement the interface `org.aktin.broker.server.auth.AuthProvider`.

