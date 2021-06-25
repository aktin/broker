#!/bin/bash

cd target/clients
for name in run_client*.sh; do
	echo Running $name in background ...
	bash $name &
done
