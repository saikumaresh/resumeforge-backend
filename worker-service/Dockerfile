# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY worker-service/pom.xml /app/worker-service/pom.xml

WORKDIR /app/worker-service
COPY worker-service/src ./src

RUN mvn clean package -DskipTests -f pom.xml

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S resumeforge && adduser -S resumeforge -G resumeforge
COPY --from=build /app/worker-service/target/*.jar app.jar
RUN chown resumeforge:resumeforge app.jar
USER resumeforge
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
