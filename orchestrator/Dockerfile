FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY nlu/pom.xml nlu/
COPY pathfinder/pom.xml pathfinder/
COPY audio/pom.xml audio/
COPY gtfs/pom.xml gtfs/
COPY orchestrator/pom.xml orchestrator/

RUN mvn -B dependency:go-offline -P gpu -pl orchestrator -am

COPY common/ common/
COPY nlu/ nlu/
COPY pathfinder/ pathfinder/
COPY audio/ audio/
COPY orchestrator/ orchestrator/

RUN mvn -B clean package -DskipTests -P gpu -pl orchestrator -am

FROM pytorch/pytorch:2.5.1-cuda12.4-cudnn9-devel

WORKDIR /app

RUN apt-get update && \
    apt-get install -y openjdk-21-jre-headless && \
    pip install mlflow && \
    rm -rf /var/lib/apt/lists/*

# Configuration NVIDIA pour DJL
ENV NVIDIA_VISIBLE_DEVICES=all
ENV NVIDIA_DRIVER_CAPABILITIES=compute,utility
ENV DJL_PYTORCH_DEVICE=gpu
ENV DJL_ENGINE=PyTorch
ENV DJL_CACHE_DIR=/app/.djl_cache

COPY --from=builder /app/orchestrator/target/orchestrator-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]