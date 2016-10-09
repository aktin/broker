Use the broker server
=====================

To use the broker server, run a JAX-RS server with
the endpoints org.aktin.broker.BrokerEndpoint and
org.aktin.broker.AggregatorEndpoint

For real-time notification of queries, use the websocket
endpoint org.aktin.broker.notify.BrokerWebsocket



Authentication
==============

TODO: API-Key authentication
REST API access is done via TLS client authentication. To use the
REST API, you need to generate a private key and request a certificate 
signed by the same CA (or sub CA) as the broker. (`CSR` = certificate signing request)

To do so, perform the following steps:

1. locate the Java `keytool` command and make sure it is accessible 
from your command line. This is usually located in your Java runtime installation
in the `bin` directory.

2. Generate a public and private key pair and store the keys in a keystore: `keytool -genkeypair -keyalg RSA -keysize 1024 -storetype pkcs12 -keystore mykeystore.p12 -storepass CreativeTopSecretPassword`

3. Import and trust the certification authority used by the broker. You can get the certificate
used by the broker by pointing your browser to https://broker-address/broker/cert (performing a GET request).
`keytool -importcert -alias ca -file ca.pem -keystore mykeystore.p12 -storepass CreativeTopSecretPassword`


Use the REST API from the command line
--------------------------------------

```
curl --cert keystore.p12:xxx123 --cert-type P12 --insecure https://blue.at.struktu.ro/idm/lala.test?asdf
```

 