FROM maven:3.9.0-eclipse-temurin-17-focal AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY . /tmp/
WORKDIR /tmp/
RUN mvn package
RUN apt update && apt install -y zip
RUN cd /tmp/broker-admin-dist/target && unzip *.zip -d deploy


FROM eclipse-temurin:17-jre-focal

WORKDIR /opt/aktin
COPY --from=MAVEN_TOOL_CHAIN /tmp/broker-admin-dist/target/deploy .
COPY docker/aktin-broker/run_broker.sh ./run_broker.sh
RUN chmod 644 run_broker.sh

ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"

RUN chown -R 10001:10001 /opt/aktin
USER 10001

ENTRYPOINT ["bash", "run_broker.sh"]

ARG GIT_REF=""
ARG BUILD_TIME=""
LABEL maintainer="aktin" \
    org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="aktin" \
    org.opencontainers.image.source="https://github.com/aktin/broker" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="aktin" \
    org.opencontainers.image.title="aktin broker" \
    org.opencontainers.image.description="Aktin broker for distribution of queries"
