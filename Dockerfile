FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app


COPY pom.xml .
COPY common/pom.xml common/
COPY nlu/pom.xml nlu/
COPY pathfinder/pom.xml pathfinder/
COPY audio/pom.xml audio/
COPY orchestrator/pom.xml orchestrator/

RUN mvn -B dependency:go-offline -P gpu


COPY common/ common/
COPY nlu/ nlu/
COPY pathfinder/ pathfinder/
COPY audio/ audio/
COPY orchestrator/ orchestrator/

RUN mvn -B clean package -DskipTests -P gpu

RUN mkdir -p /djl && \
    curl -L https://publish.djl.ai/pytorch/2.3.1/jnilib/0.36.0/linux-x86_64/cu121/libdjl_torch.so \
    -o /djl/libdjl_torch.so


FROM nvidia/cuda:12.1.1-cudnn8-runtime-ubuntu22.04

WORKDIR /app

RUN apt-get update && \
    apt-get install -y openjdk-21-jre-headless && \
    rm -rf /var/lib/apt/lists/*

ENV NVIDIA_VISIBLE_DEVICES=all
ENV NVIDIA_DRIVER_CAPABILITIES=compute,utility
ENV DJL_PYTORCH_DEVICE=gpu
ENV DJL_ENGINE=PyTorch

ENV DJL_CACHE_DIR=/app/.djl_cache

COPY --from=builder /app/orchestrator/target/orchestrator-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]