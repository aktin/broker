#!/bin/bash

cd target/clients
for name in ../../target/queries/*; do
	echo Running query $name ...
	bash run_query.sh $name
done
