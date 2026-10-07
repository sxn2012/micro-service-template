# Build stage: builds one Maven module as a runnable Spring Boot jar.
# Usage: docker build --build-arg SERVICE=user-service -t microservice/user-service:latest .
ARG SERVICE

FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app

# Copy all pom.xml first to leverage dependency cache
COPY pom.xml ./
COPY microservice-common/pom.xml microservice-common/pom.xml
COPY microservice-registry/pom.xml microservice-registry/pom.xml
COPY microservice-config/pom.xml microservice-config/pom.xml
COPY microservice-gateway/pom.xml microservice-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml

ARG SERVICE
RUN mvn -B dependency:go-offline -pl ${SERVICE} -am

# Copy all sources
COPY microservice-common/src microservice-common/src
COPY microservice-registry/src microservice-registry/src
COPY microservice-config/src microservice-config/src
COPY microservice-gateway/src microservice-gateway/src
COPY user-service/src user-service/src
COPY product-service/src product-service/src
COPY cart-service/src cart-service/src
COPY order-service/src order-service/src

ARG SERVICE
RUN mvn -B clean package -DskipTests -pl ${SERVICE} -am -Pprod

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

ARG SERVICE
COPY --from=build --chown=appuser:appgroup /app/${SERVICE}/target/*.jar /app/app.jar

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
