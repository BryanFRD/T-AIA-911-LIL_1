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
    locales \
    curl \
    unzip \
    && locale-gen en_US.UTF-8 \
    && update-locale LANG=en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install --no-cache-dir mlflow boto3

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8
ENV NVIDIA_VISIBLE_DEVICES=all
ENV NVIDIA_DRIVER_CAPABILITIES=compute,utility
ENV DJL_PYTORCH_DEVICE=gpu
ENV DJL_ENGINE=PyTorch
ENV DJL_CACHE_DIR=/app/.djl_cache
ENV JNA_ENCODING=UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Djna.encoding=UTF-8"

COPY --from=builder /app/orchestrator/target/orchestrator-*.jar app.jar

RUN curl -L -o /tmp/vosk-model.zip https://alphacephei.com/vosk/models/vosk-model-fr-0.22.zip \
    && mkdir -p audiomodels \
    && unzip /tmp/vosk-model.zip -d audiomodels/ \
    && rm /tmp/vosk-model.zip

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Djna.encoding=UTF-8", "-jar", "app.jar"]
