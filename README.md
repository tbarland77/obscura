# Obscura

[![Build](https://img.shields.io/github/actions/workflow/status/tbarland77/obscura/ci.yml?branch=main&label=build&logo=github)](https://github.com/tbarland77/obscura/actions)
[![Gradle](https://img.shields.io/badge/gradle-8.14-brightgreen.svg)](https://gradle.org)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.6-green.svg)](https://spring.io/projects/spring-boot)
[![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen.svg)](https://www.jacoco.org/)

A lightweight Spring Boot REST API for managing stories. This project demonstrates modern Spring Boot best practices including clean architecture, comprehensive test coverage, and containerization.

## Features

- **Full CRUD API** for story management
- **Input validation** with Jakarta Bean Validation
- **In-memory H2 database** for quick setup and testing
- **Spring Boot Actuator** for health checks and metrics
- **Code quality enforcement** with JaCoCo (85% coverage) and Spotless
- **Docker support** with multi-stage builds
- **Comprehensive test suite** with JUnit 5 and Mockito

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build Tool | Gradle 8.14.3 |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA / Hibernate |
| Testing | JUnit 5, Mockito |
| Code Quality | JaCoCo, Spotless (Google Java Format) |
| Containerization | Docker |

## Quick Start

### Prerequisites

- **Java 21** (or let Gradle toolchain download it automatically)
- **Docker & Docker Compose** (optional, for containerized development)

### Running Locally

#### Option 1: Gradle (Recommended for Development)

```bash
# Windows
.\gradlew.bat bootRun

# Unix/macOS/Git Bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`

#### Option 2: Docker Compose (Development Mode)

```bash
docker-compose up
```

This uses the override configuration for fast local development with live code reloading.

#### Option 3: Docker (Production-like Build)

```bash
# Build the image
docker build -t obscura:latest .

# Run the container
docker run -p 8080:8080 --rm obscura:latest
```

## API Endpoints

Base URL: `http://localhost:8080/api/stories`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/stories` | Retrieve all stories | 200 |
| POST | `/api/stories` | Create a new story | 200 |
| PUT | `/api/stories/{id}` | Update an existing story | 200 |
| DELETE | `/api/stories/{id}` | Delete a story | 204 |

### Request/Response Examples

#### Create a Story

```bash
POST /api/stories
Content-Type: application/json

{
  "title": "My First Story",
  "content": "This is the content of my story.",
  "author": "John Doe",
  "tags": ["fiction", "adventure"]
}
```

#### Response

```json
{
  "id": 1,
  "title": "My First Story",
  "content": "This is the content of my story.",
  "author": "John Doe",
  "tags": ["fiction", "adventure"],
  "createdAt": "2025-11-11T10:30:00"
}
```

### Data Validation

- `title`: Required, max 100 characters
- `content`: Required
- `author`: Required
- `tags`: Optional list of strings

## Building and Testing

### Run Tests

```bash
.\gradlew.bat test
```

Test reports are generated in `build/reports/tests/test/`

### Full Build with Coverage

```bash
.\gradlew.bat clean build
```

This runs:
- All unit tests
- JaCoCo coverage verification (minimum 85% line coverage)
- Code formatting checks with Spotless

Coverage reports are available in `build/reports/jacoco/test/html/`

### Code Formatting

```bash
# Check formatting
.\gradlew.bat spotlessCheck

# Auto-format code
.\gradlew.bat spotlessApply
```

## Project Structure

```
src/main/java/io/github/tbarland/obscura/
├── ObscuraApplication.java          # Main application entry point
├── controller/
│   └── StoryController.java         # REST API endpoints
├── service/
│   └── StoryService.java            # Business logic layer
├── repository/
│   └── StoryRepository.java         # Data access layer
├── model/
│   └── Story.java                   # JPA entity
└── dto/
    ├── StoryRequestDto.java         # Input validation DTO
    └── StoryResponseDto.java        # Response DTO

src/main/resources/
└── application.yml                  # Spring Boot configuration

src/test/java/io/github/tbarland/obscura/
├── controller/
│   └── StoryControllerTests.java
├── service/
│   └── StoryServiceTests.java
└── ObscuraApplicationTests.java
```

## Actuator Endpoints

Spring Boot Actuator exposes monitoring and management endpoints:

- `http://localhost:8080/actuator/health` - Application health status
- `http://localhost:8080/actuator/info` - Application information
- `http://localhost:8080/actuator/metrics` - Application metrics

## Docker Configuration

### Development Setup

The project includes two Docker Compose files:

- **docker-compose.yml**: Base configuration that builds a production-like image
- **docker-compose.override.yml**: Development overrides with:
  - Live code mounting for hot reload
  - Gradle cache persistence
  - Spring DevTools enabled

When you run `docker-compose up`, both files are automatically merged for the best local development experience.

### Production Dockerfile

The Dockerfile uses a multi-stage build:

1. **Build stage**: Compiles and packages the application using Eclipse Temurin 21 JDK
2. **Runtime stage**: Creates a minimal image with only the JRE and JAR file
3. Runs as non-root user for security
4. Optimized for smaller image size

## Development Notes

- **Java Toolchain**: The project enforces Java 21 via Gradle toolchain. Gradle will attempt to download it if not available.
- **Code Style**: Uses Google Java Format enforced by Spotless. Run `./gradlew spotlessApply` before committing.
- **Test Coverage**: Build fails if line coverage drops below 85%. Adjust in `build.gradle` if needed.
- **Database**: H2 runs in-memory by default. Data is lost on restart. Enable H2 console in `application.yml` if needed for debugging.

## Troubleshooting

### Gradle Issues

**Problem**: Gradle fails due to missing JDK
```bash
# Solution: Install JDK 21 or adjust toolchain in build.gradle
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
```

### Docker Compose Issues

**Problem**: Port 8080 already in use
```bash
# Solution: Stop other services or change port mapping in docker-compose.yml
ports:
  - "8081:8080"
```

### H2 Database

**Problem**: Need to inspect database contents

**Solution**: Enable H2 console in `application.yml`:
```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

Then access at `http://localhost:8080/h2-console`

## Contributing

Contributions are welcome! Please ensure:

1. All tests pass: `./gradlew test`
2. Code coverage meets minimum threshold (85%)
3. Code is formatted: `./gradlew spotlessApply`
4. Build succeeds: `./gradlew clean build`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
