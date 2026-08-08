# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY api-gateway/pom.xml /app/api-gateway/pom.xml

WORKDIR /app/api-gateway
COPY api-gateway/src ./src

RUN mvn clean package -DskipTests -f pom.xml

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S resumeforge && adduser -S resumeforge -G resumeforge
COPY --from=build /app/api-gateway/target/*.jar app.jar
RUN chown resumeforge:resumeforge app.jar
USER resumeforge
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
