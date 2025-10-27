# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy pom.xml
COPY pom.xml ./

# Install Maven, download dependencies (Maven remains installed for the next step)
# Cache apt lists cleanup for later
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B

# Copy the project source code into the container
# This layer is invalidated if source code changes
COPY src ./src

# Build the application using Maven (mvn command is now available)
# Then cleanup maven and apt cache
RUN mvn package -DskipTests && \
    apt-get purge -y --auto-remove maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Expose the port the app runs on
EXPOSE 8080

# Define the command to run your application
ENTRYPOINT ["java", "-jar", "target/venueconnect-0.0.1-SNAPSHOT.jar"]