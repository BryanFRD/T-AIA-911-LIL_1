# Travel Order Resolver 🚂

AI-powered modular system to process natural language voice commands for train travel planning.

## 📋 Project Overview

Multi-module Maven project that processes voice commands to plan train journeys:
- 🎤 Voice input → Text (STT)
- 🧠 Text → Intent understanding (NLU)  
- 🗺️ Route calculation (Pathfinding)
- 🔊 Text → Voice output (TTS)
- 🌐 REST API orchestration
- 📊 GTFS data processing (SNCF travel data)

## 🏗️ Architecture

This project follows **Hexagonal Architecture (Ports & Adapters)** principles for:
- ✅ Clean separation of concerns
- ✅ High testability
- ✅ Flexibility and maintainability
- ✅ Independence from frameworks

### 📚 Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Complete architecture documentation
- **[ARCHITECTURE_DIAGRAMS.md](./ARCHITECTURE_DIAGRAMS.md)** - Visual diagrams and flows
- **[REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md)** - Refactoring details and changes

## 🧩 Modules

```
travelorder-parent/
├── common/          # Shared domain models & utilities
├── nlu/            # Natural Language Understanding (DJL + ML models)
│   ├── domain/     #   - Ports (Use Cases & SPIs)
│   ├── application/#   - Use Case implementations
│   └── infrastructure/ - DJL adapters
├── audio/          # Speech-to-Text / Text-to-Speech
├── pathfinder/     # Route finding algorithms (GTFS + Dijkstra)
│   ├── domain/     #   - Ports & business logic
│   ├── application/#   - Use Case implementations
│   └── infrastructure/ - Repository adapters
├── gtfs/           # GTFS data ingestion (Spring Batch)
└── orchestrator/   # REST API orchestration (Spring Boot)
    ├── domain/     #   - Ports & business models
    ├── application/#   - Use Case implementations
    └── infrastructure/ - REST controllers & adapters
```

## 🔧 Prerequisites

- **Java 21** (Oracle JDK)
- **Maven 3.9.9+**
- **Docker** (optional, for PostgreSQL)
- **SDKMAN** (optional, for Java version management)

## 🚀 Quick Start

### 1. Build Project

```bash
# Build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### 2. Run Orchestrator (API)

```bash
cd orchestrator
mvn spring-boot:run
```

API available at: `http://localhost:8080`

### 3. Endpoints

#### Transcribe Audio
```bash
POST /travel-order/transcribe
Content-Type: multipart/form-data

audio: <audio-file>
```

#### Find Travel Path
```bash
POST /travel-order/find
Content-Type: application/json

{
  "customerPath": "Je veux aller de Lyon à Paris"
}
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl orchestrator

# Integration tests
mvn verify
```

## 📊 Code Quality

```bash
# Checkstyle validation
mvn checkstyle:check

# Code coverage (JaCoCo)
mvn jacoco:report
```

## 🐳 Docker

```bash
# Build Docker images
docker build -t travel-order-orchestrator -f orchestrator/Dockerfile .
docker build -t travel-order-gtfs -f gtfs/Dockerfile .

# Run with Docker Compose (if available)
docker-compose up
```

## 📖 API Documentation

Once the orchestrator is running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## 🛠️ Development

### Project Structure

```
src/main/java/
├── domain/          # Business logic (framework-agnostic)
│   ├── model/       # Domain entities
│   ├── port/        # Interfaces (Use Cases & SPIs)
│   └── service/     # Pure business services
├── application/     # Use case implementations
│   └── usecase/
└── infrastructure/  # Technical implementations
    ├── adapter/
    │   ├── in/      # Input adapters (REST, CLI)
    │   └── out/     # Output adapters (DB, HTTP clients)
    └── config/      # Framework configuration
```

### Adding a New Feature

1. Define port in `domain/port/in/` (use case interface)
2. Define required external dependencies in `domain/port/out/`
3. Implement use case in `application/usecase/`
4. Create adapters in `infrastructure/adapter/`
5. Wire beans in Spring configuration

## 🔐 Environment Variables

```bash
# MLFlow configuration
MLFLOW_TRACKING_URI=http://localhost:5000
MLFLOW_MODEL_ALIAS=champion
MLFLOW_MODEL_CACHE_DIR=./models

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/travelorder
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
```

## 📦 Build Artifacts

After `mvn clean install`, artifacts are available in:
```
orchestrator/target/orchestrator-1.0.0.jar
gtfs/target/gtfs-1.0.0.jar
nlu/target/nlu-1.0.0.jar
pathfinder/target/pathfinder-1.0.0.jar
audio/target/audio-1.0.0.jar
```

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Follow hexagonal architecture principles
4. Write tests (unit + integration)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 👥 Authors

- **Arthur Azoula** - [GitHub](https://github.com/ArthurAzoula)
- **Bryan Ferrando** - [GitHub](https://github.com/BryanFRD)
- **Benjamin Kliewer** - [GitHub](https://github.com/Benjaminkli)
- **Tiago Valinhas** - [GitHub](https://github.com/tiagovalinhas)
- **William Smith** - [GitHub](https://github.com/WillSmith351)

## 📄 License

This project is part of Epitech T-AIA-901 module.

## 🙏 Acknowledgments

- SNCF for GTFS data
- Deep Java Library (DJL) for ML inference
- Spring Boot & Spring Batch
- Hexagonal Architecture by Alistair Cockburn
- Clean Architecture by Robert C. Martin
