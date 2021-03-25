#!/bin/sh

export COMPOSE_PROJECT=codex-develop

docker-compose -p $COMPOSE_PROJECT -f docker-compose.broker.yml down
docker-compose -p $COMPOSE_PROJECT -f docker-compose.client.yml down