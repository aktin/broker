FROM maven:3.8.3-openjdk-17 AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY . /tmp/
WORKDIR /tmp/
RUN mvn clean install


FROM eclipse-temurin:17-jre-focal

WORKDIR /opt/aktin
COPY --from=MAVEN_TOOL_CHAIN /tmp/broker-client/target/*.jar ./lib/
COPY --from=MAVEN_TOOL_CHAIN /tmp/broker-client/target/lib ./lib/
COPY --from=MAVEN_TOOL_CHAIN /tmp/broker-client/src/test/resources/sysproc.properties .
COPY docker/aktin-client/run_client.sh ./run_client.sh
COPY docker/aktin-client/logging.properties ./logging.properties
RUN chmod 644 run_client.sh

ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"


COPY docker/aktin-client/sysproc.properties sysproc.properties
RUN chown -R 10001:10001 /opt/aktin
USER 10001

ENTRYPOINT ["bash", "run_client.sh"]

ARG GIT_REF=""
ARG BUILD_TIME=""
LABEL maintainer="aktin" \
    org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="aktin" \
    org.opencontainers.image.source="https://github.com/aktin/broker" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="aktin" \
    org.opencontainers.image.title="aktin client" \
    org.opencontainers.image.description="Aktin client for execution of queries"
