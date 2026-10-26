# syntax=docker/dockerfile:1

# Three stages, and the middle one is the interesting part.
#
# The AOT cache is only valid for the exact application build and the exact JVM that
# produced it, so it cannot be built on a developer machine and shipped. It has to be
# generated inside this image, against the jar this image will run, by the JVM this image
# will run it with. That is why the training and runtime stages share one base image tag:
# changing either independently silently invalidates the cache.
#
# Measured on this application, median of three runs each way:
#
#   in a 512 MB / 1 CPU container   9.10 s  ->  4.22 s   53.6% faster
#   on an unconstrained host        4.17 s  ->  2.34 s   43.8% faster
#
# The container figure is the one that matters, and it is the larger of the two because a
# constrained CPU makes class loading and linking a bigger share of startup. The cost is
# 106 MB of image. On an always-warm deployment this buys faster blue-green cutovers and
# restarts rather than faster request handling, which is a trade worth stating rather than
# assuming.

# ---------------------------------------------------------------------------- build ---
FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

# Dependencies resolve in their own layer so a source-only change does not re-download
# the world. The wrapper pins the Maven version, so this build uses the same Maven a
# developer does.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B --no-transfer-progress dependency:go-offline

COPY src/ src/
# maven.test.skip rather than skipTests, because .dockerignore excludes the test sources
# entirely. Tests run in CI against real Postgres, which is not available here and should
# not be faked with an embedded substitute just to make a container build feel thorough.
RUN ./mvnw -B --no-transfer-progress package -Dmaven.test.skip=true

# ------------------------------------------------------------------------ aot cache ---
FROM eclipse-temurin:25-jre AS aot

WORKDIR /stage
COPY --from=build /build/target/*.jar app.jar

# The extracted layout, not the fat jar. Spring Boot's AOT cache has no effect on a
# packaged jar, and running from the extracted form is faster regardless.
RUN java -Djarmode=tools -jar app.jar extract --destination application

WORKDIR /stage/application
# The training run starts the application context and exits at refresh. No database is
# available at image build time, so Flyway is off, Hikari is told not to fail when it
# cannot connect, and the Hibernate dialect is stated outright rather than detected from
# a live connection. None of these settings reach the runtime stage.
RUN java -XX:AOTCacheOutput=/stage/app.aot \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -jar app.jar \
  && test -s /stage/app.aot

# -------------------------------------------------------------------------- runtime ---
FROM eclipse-temurin:25-jre AS runtime

# Unprivileged, with no login shell. If this process is ever compromised, it should be a
# boring foothold rather than an interesting one.
RUN groupadd --system --gid 10001 wayfinder \
  && useradd --system --uid 10001 --gid wayfinder --no-create-home --shell /usr/sbin/nologin wayfinder

WORKDIR /app
COPY --from=aot --chown=wayfinder:wayfinder /stage/application/ ./
COPY --from=aot --chown=wayfinder:wayfinder /stage/app.aot /app/app.aot

USER wayfinder:wayfinder
EXPOSE 8080

ENV JAVA_OPTS=""

# MaxRAMPercentage rather than a fixed heap, because the container's limit is the only
# number that is actually known here. Held at 55 rather than the usual 75: metaspace,
# thread stacks, code cache and the mapped AOT cache all live outside the heap, and on a
# small instance that overhead is a large fraction of the total.
#
# ExitOnOutOfMemoryError so the platform restarts a wedged process instead of leaving it
# to thrash. GC choice is left to the JVM, which already selects a small-footprint
# collector when the container is this size.
#
# The trailing "$@" and the "--" placeholder matter: a platform that runs a one-off
# command against this image (Fly's release_command, for instance) does not override
# ENTRYPOINT, so without argument forwarding the migration command would be silently
# dropped and the deploy would proceed against an unmigrated schema.
ENTRYPOINT ["sh", "-c", "exec java \
  -XX:AOTCache=/app/app.aot \
  -XX:MaxRAMPercentage=55 \
  -XX:MaxMetaspaceSize=160m \
  -XX:+ExitOnOutOfMemoryError \
  $JAVA_OPTS \
  -jar /app/app.jar \"$@\"", "--"]
