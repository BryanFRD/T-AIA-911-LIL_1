# Travel Order Resolver

AI-powered modular system to process natural language voice commands for train travel planning.

## Project Overview

Multi-module Maven project that processes voice commands to plan train journeys:
- Voice input → Text (STT)
- Text → Intent understanding (NLU)  
- Route calculation (Pathfinding)
- Text → Voice output (TTS)
- REST API orchestration

## Modules

```
travelorder-parent/
├── common/          # Shared models & utilities
├── nlu/            # Natural Language Understanding (DJL)
├── audio/          # Text-to-Speech / Speech-to-Text
├── pathfinder/     # Route finding algorithms
└── orchestrator/   # REST API (Spring Boot)
```

## Prerequisites

- Java 25 (Eclipse Temurin)
- Maven 3.9.9+
- SDKMAN (optional)

## Setup

```bash
# Build all modules
mvn clean install
```

## Authors

- Arthur Azoula - [GitHub]('https://github.com/ArthurAzoula')
- Bryan Ferrando - [GitHub]('https://github.com/BryanFRD')
- Benjamin Kliewer - [GitHub]('https://github.com/Benjaminkli')
- Tiago Valinhas - [GitHub]('https://github.com/Benjaminkli')
- William Smith - [GitHub]('https://github.com/WillSmith351')