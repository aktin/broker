# Docker Version of the aktin broker and client





## Run docker setup

To run the docker setup on one local machine execute the following commands:

```bash
export COMPOSE_PROJECT=example-project

docker-compose -p $COMPOSE_PROJECT -f docker-compose.broker.yml up -d
sleep 10
docker-compose -p $COMPOSE_PROJECT -f docker-compose.client.yml up -d
```

Once started visit the admin at:

http://localhost:8080/admin/html/login.html

user: admin
password: from AKTIN_ADMIN_PW environment variable - see above
