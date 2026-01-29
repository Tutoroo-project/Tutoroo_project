FROM eclipse-temurin:21-jdk
ARG JAR_FILE=target/*.jar
ARG PROFILE
WORKDIR /app
COPY uploads /app/uploads
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=${PROFILE}", "-jar", "/app,.jar"]
# java -jar ~~~~.jar