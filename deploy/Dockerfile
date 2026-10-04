# syntax=docker/dockerfile:1.7
# Declared before the first FROM so they can be used in the FROM lines
ARG JAVA_VERSION=21
# JDK that compiles the module. Use 17 for the modules that target Java 17 (storage-service, config-service,
# discovery-service): they run on Spring Boot 3.1 whose Lombok does not work with the JDK 21 compiler
# ("NoSuchFieldError: JCTree$JCImport ... qualid")
ARG BUILD_JDK=21
#
# One Dockerfile for every Spring Boot service of this repository. Build from the repository root:
#
#   docker build -f deploy/Dockerfile --build-arg MODULE=chat-service -t zapoo/chat-service .
#
# Build args
#   MODULE          Maven module to build (api-gateway, chat-service, ...)
#   BUILD_JDK       JDK used to compile (21 by default, 17 for the Java 17 modules, see below)
#   JAVA_VERSION    Java of the runtime image. 21 for most services, 17 for config-service, discovery-service and
#                   storage-service (their poms target Java 17)
#   INSTALL_FFMPEG  "true" for storage-service, it cuts video thumbnails and transcodes videos with ffmpeg

FROM maven:3.9-eclipse-temurin-${BUILD_JDK} AS build
ARG MODULE
WORKDIR /src
COPY . .
# -am also builds the modules MODULE depends on (common-lib). Tests are not run here and spotless must not rewrite
# sources inside the build. The .m2 cache makes rebuilds fast.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q -pl "${MODULE}" -am -DskipTests -Dspotless.apply.skip=true package \
    && cp "${MODULE}"/target/*.jar /app.jar

FROM eclipse-temurin:${JAVA_VERSION}-jre
ARG INSTALL_FFMPEG=false
RUN if [ "${INSTALL_FFMPEG}" = "true" ]; then \
        apt-get update \
        && apt-get install -y --no-install-recommends ffmpeg \
        && rm -rf /var/lib/apt/lists/*; \
    fi \
    && useradd --system --create-home --uid 10001 app
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER app

# Containers get a fixed share of the machine's memory, let the JVM size its heap from that limit
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
