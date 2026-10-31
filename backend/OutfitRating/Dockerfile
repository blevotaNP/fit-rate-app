# --- build stage -------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

# Copy the wrapper first so the Gradle distribution and the dependency graph are cached in their
# own layer; source changes then rebuild without re-downloading the world.
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
# The repository may carry CRLF line endings from a Windows checkout.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Uploads land on a mounted volume, so the container itself stays disposable.
RUN addgroup -S outfit && adduser -S outfit -G outfit \
    && mkdir -p /var/outfitrating/uploads \
    && chown -R outfit:outfit /var/outfitrating

COPY --from=build /build/build/libs/*.jar app.jar

USER outfit
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
