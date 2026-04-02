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

# Resolve dependencies for payment-service
WORKDIR /app/payment-service
RUN mvn dependency:resolve

# Build
COPY payment-service/src ./src
WORKDIR /app
RUN mvn -pl payment-service clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=builder /app/payment-service/target/payment-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
