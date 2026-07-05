# Step 1: Use official Maven image to compile code safely
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Step 2: Copy only the configuration and source code
COPY pom.xml .
COPY src ./src

# Step 3: Run native package build command
RUN mvn clean package -DskipTests

# Step 4: Create final minimal lightweight execution image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/sql-query-agent-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
