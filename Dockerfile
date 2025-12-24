FROM maven:3.9.12-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy only the pom files to leverage Docker layer caching
COPY pom.xml .
COPY common/pom.xml common/
COPY nlu/pom.xml nlu/
COPY pathfinder/pom.xml pathfinder/
COPY audio/pom.xml audio/
COPY orchestrator/pom.xml orchestrator/

RUN mvn -B dependency:go-offline

# Copy all source code
COPY common/ common/
COPY nlu/ nlu/
COPY pathfinder/ pathfinder/
COPY audio/ audio/
COPY orchestrator/ orchestrator/

# Build all modules without tests
RUN mvn -B clean install -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /app/orchestrator/target/orchestrator-*.jar orchestrator.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "orchestrator.jar"]