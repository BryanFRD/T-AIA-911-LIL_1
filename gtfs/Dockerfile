FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY gtfs/pom.xml gtfs/

RUN mvn -B dependency:go-offline

COPY common/ common/
COPY gtfs/ gtfs/

RUN mvn -B clean package -DskipTests -pl gtfs -am

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/gtfs/target/gtfs-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]