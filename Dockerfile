FROM gradle:jdk17-alpine AS builder

WORKDIR /openai-service

COPY --chown=gradle:gradle openai-service/build.gradle settings.gradle /openai-service/
COPY --chown=gradle:gradle openai-service/src /openai-service/src
COPY --chown=gradle:gradle gradle.properties /openai-service/gradle.properties

RUN gradle --no-daemon build 

# Use a Java base image
FROM openjdk:17-alpine

# Set the working directory 
WORKDIR /src

# Copy the Spring Boot application JAR file into the Docker image
COPY --from=builder /openai-service/build/libs/*.jar /src/services.openAIService-2.0.0.jar

# Set environment variables
ENV SERVER_PORT=8080
ENV ISSUER_URI=https://auth.las2peer.org/auth/realms/main 
ENV SET_URI=https://auth.las2peer.org/auth/realms/main/protocol/openid-connect/certs

# Expose the port that the Spring Boot application is listening on
EXPOSE 8080

# Entry point to run the Spring Boot application
ENTRYPOINT ["java","-jar","/src/services.openAIService-2.0.0.jar", "--spring.security.oauth2.resourceserver.jwt.issuer-uri=${ISSUER_URI}", "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${SET_URI}"]