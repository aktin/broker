#!/bin/bash

CLIENTS=5
REQUESTS=50
MEDIATYPE=text/plain+x.new.request

rm -rf target/server
rm -rf target/clients

# generate queries
# don't generate if queries folder already exists
if [ ! -d target/queries ]; then
	mkdir -p target/queries
	for i in $(seq 1 $REQUESTS); do
		iwithzeros=$(printf "%03d" $i)
		echo query content $i > target/queries/query$iwithzeros.txt
	done
fi

# run broker
mkdir -p target/server
cd target/server
unzip ../../../broker-admin-dist/target/*.zip

# fill api keys
echo > api-keys.properties
for i in $(seq 1 $CLIENTS); do
	echo loadTestClient$i=CN=Client$i >> api-keys.properties
done
echo xxxAdmin1234=CN=Admin,O=AKTIN,OU=admin >> api-keys.properties

# prepare clients
cd ..
mkdir -p clients
cd clients
cp -r ../../../broker-client/target/lib ./lib
cp  ../../../broker-client/target/*.jar ./lib
cp  ../../../broker-client/src/test/resources/sysproc.properties ./
cp  ../../../broker-client/src/test/resources/echo1.sh ./

cat sysproc.properties | sed "s|/usr/bin/sleep|/bin/bash|" | sed "/process.args=/ s/=.*/=echo1.sh/" | sed "/broker.request.mediatype=/ s|=.*|=$MEDIATYPE|" > sysproc.template

for i in $(seq 1 $CLIENTS); do
	sed "/client.auth.param=/ s/=.*/=loadTestClient$i/" sysproc.template > sysproc$i.properties
	echo java -cp lib/\\\* org.aktin.broker.client.live.sysproc.CLI sysproc$i.properties > run_client$i.sh
done

# write query runner
echo java -cp lib/\\\* org.aktin.broker.client.live.runquery.CLI http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234 $MEDIATYPE \$1 > run_query.sh


