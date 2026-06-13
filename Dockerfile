# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copy ONLY pom.xml to cache dependencies
COPY pom.xml .
# 2. Go offline to download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# 3. Now copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]