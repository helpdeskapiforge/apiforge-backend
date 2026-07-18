# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -Dmaven.test.skip=true

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is needed for the HEALTHCHECK below; the base image doesn't include it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user: the previous image ran the JVM as root inside the container,
# which is unnecessary privilege if the container is ever compromised.
RUN groupadd -r apiforge && useradd -r -g apiforge apiforge
COPY --from=build /app/target/*.jar app.jar
RUN chown apiforge:apiforge app.jar
USER apiforge

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
