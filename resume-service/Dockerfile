# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM first for layer caching
COPY pom.xml /app/parent-pom.xml
COPY resume-service/pom.xml /app/resume-service/pom.xml

WORKDIR /app/resume-service
COPY resume-service/src ./src

RUN mvn clean package -DskipTests -f pom.xml

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S resumeforge && adduser -S resumeforge -G resumeforge
COPY --from=build /app/resume-service/target/*.jar app.jar
RUN chown resumeforge:resumeforge app.jar
USER resumeforge
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
