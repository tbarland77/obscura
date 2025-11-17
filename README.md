# Obscura

[![Build](https://img.shields.io/github/actions/workflow/status/tbarland77/obscura/ci.yml?branch=main&label=build&logo=github)](https://github.com/tbarland77/obscura/actions)
[![Gradle](https://img.shields.io/badge/gradle-8.14-brightgreen.svg)](https://gradle.org)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.6-green.svg)](https://spring.io/projects/spring-boot)
[![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen.svg)](https://www.jacoco.org/)

A lightweight Spring Boot REST API for managing stories. This project demonstrates modern Spring Boot best practices including clean architecture, comprehensive test coverage, and containerization.

## Features

- **Full CRUD API** for story management with horror/suspense themed content
- **Input validation** with Jakarta Bean Validation
- **Database migrations** with Flyway for version-controlled schema management
- **Multi-database support** - H2 (local/test) and PostgreSQL (production)
- **Production-parity testing** with Testcontainers and PostgreSQL
- **Spring Boot Actuator** for health checks and metrics
- **Code quality enforcement** with JaCoCo (85% coverage) and Spotless
- **Docker support** with multi-stage builds
- **Comprehensive test suite** with JUnit 5, Mockito, and integration tests
- **Optimized CI/CD pipeline** with parallel jobs, build caching, and automated security alerts

## Prerequisites

### Prerequisites

- **Java 21** (or let Gradle toolchain download it automatically)
- **Docker & Docker Compose** (optional, for containerized development and Testcontainers tests)

### Running Locally

#### Option 1: Gradle (Recommended for Development)

```bash
# Windows
.\gradlew.bat bootRun

# Unix/macOS/Git Bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`

#### Option 2: Docker Compose (Development Mode with H2)

```bash
docker-compose up
```

This uses the override configuration for fast local development with live code reloading and H2 in-memory database.

#### Option 3: Docker Compose with PostgreSQL (Production-like)

```bash
# Production-like build with PostgreSQL
docker-compose -f docker-compose.postgres.yml up

# Development mode with PostgreSQL and hot reload
docker-compose -f docker-compose.postgres-dev.yml up
```

This spins up both PostgreSQL 17 and the application, mimicking production environment locally.

#### Option 4: Docker (Production-like Build)

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
# Run all tests (requires Docker for PostgreSQL integration tests)
.\gradlew.bat test

# Run tests excluding integration tests (no Docker required - fast)
.\gradlew.bat test -Dtest.excludeTags=integration

# Run only unit tests (fast feedback)
.\gradlew.bat test --tests StoryServiceTests --tests StoryControllerTests

# Run Flyway migration tests (H2-based, no Docker required)
.\gradlew.bat test --tests FlywayMigrationTests

# Run only integration tests (requires Docker)
.\gradlew.bat test -Dtest.includeTags=integration
```

Test reports are generated in `build/reports/tests/test/`

### Test Suite Overview

The project includes three layers of testing:

1. **Unit Tests** (`StoryServiceTests`, `StoryControllerTests`)
   - Fast, isolated tests using mocks
   - No database required
   - Test business logic and controller behavior

2. **Flyway Integration Tests** (`FlywayMigrationTests`)
   - Tests Flyway migrations against H2
   - Verifies schema creation and JPA compatibility
   - No Docker required

3. **Production-Parity Integration Tests** (`PostgresIntegrationTests`)
   - Uses Testcontainers to spin up real PostgreSQL container
   - Tests full REST API against PostgreSQL
   - Verifies production-specific behavior (BIGSERIAL, TEXT columns, etc.)
   - **Requires Docker to be running**

### Full Build with Coverage

```bash
.\gradlew.bat clean build
```

This runs:
- All unit tests
- Flyway migration tests
- PostgreSQL integration tests (if Docker available)
- JaCoCo coverage verification (minimum 85% line coverage)
- Code formatting checks with Spotless

Coverage reports are available in `build/reports/jacoco/test/html/`

### Performance Optimizations

The project includes build performance optimizations in `gradle.properties`:

- **Build cache** - Reuses outputs from previous builds
- **Parallel execution** - Runs independent tasks concurrently
- **Configuration cache** - Speeds up Gradle configuration phase

These optimizations significantly reduce build times, especially for incremental builds. The CI/CD pipeline also leverages these features for faster feedback.

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
├── application.yml                  # Base Spring Boot configuration
├── application-local.yml            # Local profile (H2 with console)
├── application-test.yml             # Test profile (H2 optimized)
├── application-prod.yml             # Production profile (PostgreSQL)
└── db/migration/
    └── V1__create_story_schema.sql  # Flyway migration script

src/test/java/io/github/tbarland/obscura/
├── controller/
│   └── StoryControllerTests.java    # Controller unit tests
├── service/
│   └── StoryServiceTests.java       # Service unit tests
├── migration/
│   └── FlywayMigrationTests.java    # Flyway integration tests (H2)
├── integration/
│   └── PostgresIntegrationTests.java # PostgreSQL integration tests (Testcontainers)
└── ObscuraApplicationTests.java     # Application context smoke test
```

## Actuator Endpoints

Spring Boot Actuator exposes monitoring and management endpoints:

- `http://localhost:8080/actuator/health` - Application health status
- `http://localhost:8080/actuator/info` - Application information
- `http://localhost:8080/actuator/metrics` - Application metrics

## Docker Configuration

### Docker Compose Files

The project includes multiple Docker Compose configurations:

#### H2 Development (Default)
- **docker-compose.yml**: Base configuration with H2 database
- **docker-compose.override.yml**: Development overrides with live code mounting and hot reload

When you run `docker-compose up`, both files are automatically merged for fast local development.

#### PostgreSQL (Production-like)
- **docker-compose.postgres.yml**: Production-like setup with PostgreSQL 17
  - Uses production profile with real PostgreSQL database
  - Builds application Docker image
  - Includes health checks and proper dependency ordering
  - Data persisted in Docker volume

- **docker-compose.postgres-dev.yml**: Development setup with PostgreSQL
  - Live code mounting for hot reload
  - Uses PostgreSQL 17 (matches production)
  - Gradle cache persistence
  - Spring DevTools enabled
  - Best for testing Flyway migrations locally

**Quick Usage:**
```bash
# H2 development (fast iteration)
docker-compose up

# PostgreSQL production-like
docker-compose -f docker-compose.postgres.yml up

# PostgreSQL development (hot reload)
docker-compose -f docker-compose.postgres-dev.yml up

# Stop and remove volumes (clean database)
docker-compose -f docker-compose.postgres.yml down -v
```

📖 **See [DOCKER_COMPOSE_GUIDE.md](DOCKER_COMPOSE_GUIDE.md) for detailed usage, troubleshooting, and workflow recommendations.**

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
- **Database Migrations**: Schema is managed by Flyway. Migration scripts are in `src/main/resources/db/migration/`. Hibernate uses `ddl-auto: validate` to ensure entities match schema.
- **Database Profiles**:
  - Default/Local/Test: H2 in-memory (data resets on restart)
  - Production: PostgreSQL (requires `DB_PASSWORD` environment variable)
- **Testcontainers**: PostgreSQL integration tests require Docker. They're automatically skipped if Docker is not running.

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

### Testcontainers Tests Failing

**Problem**: PostgreSQL integration tests fail with `DockerClientProviderStrategy` error

**Solution**: Ensure Docker is installed and running:
```bash
# Check if Docker is running
docker ps

# If not running, start Docker Desktop (Windows/macOS) or Docker daemon (Linux)
```

**Alternative**: Skip Testcontainers tests if Docker is not available:
```bash
.\gradlew.bat test --tests '*Tests' -x PostgresIntegrationTests
```

The H2-based integration tests (`FlywayMigrationTests`) provide good coverage without requiring Docker.

## Contributing

Contributions are welcome! Please ensure:

1. All tests pass: `./gradlew test`
2. Code coverage meets minimum threshold (85%)
3. Code is formatted: `./gradlew spotlessApply`
4. Build succeeds: `./gradlew clean build`

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details. You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.
