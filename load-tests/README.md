Manual load testing

1. Build the project from source with `mvn clean install`
2. Change prepare_test.sh for the desired load
3. Run `./prepare_test.sh`
4. Run the broker via `./run_broker.sh`
5. Optionally, run the admin listener
6. Run clients either with `./run_clients.sh` which starts clients as background processes or start the clients manually, via the scripts `target/clients/run_client*.sh`
7. Run queries via `./run_queries.sh`. This step can be repeated any number of times

Result of the load test must be checked manually. Automation may follow later.