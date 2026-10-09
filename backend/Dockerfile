#AUTOMATIC DEPLOY
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Spring recibe el puerto asignado por Render sin modificar application.properties.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
