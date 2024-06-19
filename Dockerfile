# Use a base image containing Java 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy of the generated JAR into the container
COPY target/tourguide-0.0.1-SNAPSHOT.jar /app/tourguide.jar

# Command to execute
CMD ["java", "-jar", "/app/tourguide.jar"]