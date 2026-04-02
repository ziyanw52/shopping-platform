# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy parent pom and all module poms
COPY pom.xml .
COPY item-service/pom.xml ./item-service/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY account-service/pom.xml ./account-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# Resolve dependencies for item-service
WORKDIR /app/item-service
RUN mvn dependency:resolve

# Build item-service only
COPY item-service/src ./src
WORKDIR /app
RUN mvn -pl item-service clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=builder /app/item-service/target/item-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
