#!/bin/sh

export COMPOSE_PROJECT=codex-develop

docker-compose -p $COMPOSE_PROJECT -f docker-compose.broker.yml up -d
sleep 10
docker-compose -p $COMPOSE_PROJECT -f docker-compose.client.yml up -d