FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/leave-service-1.0.0.jar app.jar
RUN mkdir -p /app/logs
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
