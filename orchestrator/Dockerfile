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

FROM nvidia/cuda:12.1.1-cudnn8-runtime-ubuntu22.04

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-21-jre-headless \
    python3 \
    python3-pip \
    python-is-python3 \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install --no-cache-dir mlflow boto3

ENV NVIDIA_VISIBLE_DEVICES=all
ENV NVIDIA_DRIVER_CAPABILITIES=compute,utility
ENV DJL_PYTORCH_DEVICE=gpu
ENV DJL_ENGINE=PyTorch
ENV DJL_CACHE_DIR=/app/.djl_cache
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY --from=builder /app/orchestrator/target/orchestrator-*.jar app.jar

COPY audiomodels/ audiomodels/

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]