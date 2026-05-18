FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/auth-service-1.0.0.jar app.jar
RUN mkdir -p /app/logs
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
