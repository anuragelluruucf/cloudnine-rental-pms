FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/cloudnine-0.1.0.jar app.jar
EXPOSE 8088
HEALTHCHECK --interval=15s --timeout=5s --retries=20 CMD curl -fsS http://127.0.0.1:8088/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
