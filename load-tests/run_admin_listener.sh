#!/bin/bash

cd target/clients
exec java -cp lib/\* org.aktin.broker.client.live.util.AdminListener http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234

