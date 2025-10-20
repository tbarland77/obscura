# Obscura

[![Build](https://img.shields.io/github/actions/workflow/status/tbarland77/obscura/ci.yml?branch=main&label=build&logo=github)](https://github.com/tbarland77/obscura/actions)
[![Gradle](https://img.shields.io/badge/gradle-7.x-brightgreen.svg)](https://gradle.org)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.6-green.svg)](https://spring.io/projects/spring-boot)

A small demo Spring Boot application that manages "stories" via a simple REST API. It uses Spring Data JPA, validation, an in-memory H2 database for runtime, and includes test coverage checks via JaCoCo.

## Quick facts

- Java: 21 (configured via Gradle toolchain)
- Spring Boot: 3.5.6
- Build tool: Gradle (wrapper included)
- In-memory DB: H2 (runtime)

## Badges

The badges above use Shields.io and GitHub Actions links where possible. If you don't have the referenced GitHub Actions workflow in this repository yet, the build badge will show an unknown state until a workflow is added.

## Prerequisites

- JDK 21 installed (or let Gradle use a configured toolchain that will download an appropriate JDK)
- Docker & Docker Compose (optional, for container runs)

## Running locally (recommended)

Open a terminal in the project root and use the Gradle wrapper. On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Or with Unix / Git Bash:

```bash
./gradlew bootRun
```

The application will start on the default Spring Boot port (8080) unless configured otherwise in `src/main/resources/application.yml`.

## Build and test

Run the full build and tests (this will also run JaCoCo coverage verification):

```powershell
.\gradlew.bat clean build
```

To run tests only:

```powershell
.\gradlew.bat test
```

Reports (including JaCoCo) will be generated under `build/reports/`.

## Docker

Build the image with Docker (from project root):

```powershell
docker build -t obscura:latest .
```

Run with Docker:

```powershell
docker run -p 8080:8080 --rm obscura:latest
```

Or use Docker Compose if you prefer the provided compose files:

```powershell
docker-compose up --build
```

## API Endpoints

This service exposes a small REST API under `/api/stories` (see `src/main/java/io/github/tbarland/obscura/controller/StoryController.java`):

- GET  /api/stories         — List all stories
- POST /api/stories         — Create a story (accepts JSON body)
- PUT  /api/stories/{id}    — Update a story by id (accepts JSON body)
- DELETE /api/stories/{id} — Delete a story by id

Payload DTOs are available in `src/main/java/io/github/tbarland/obscura/dto`.

## Development notes

- The project enforces a Java 21 toolchain in `build.gradle`. The Gradle wrapper will try to use it; ensure your environment allows the toolchain to download or have JDK 21 installed.
- Spotless with google-java-format is configured for code formatting.
- JaCoCo coverage verification is enabled (line coverage minimum 85%) in the build — adjust in `build.gradle` if needed for local development.

## Troubleshooting

- If Gradle fails due to missing JDK, install JDK 21 or adjust the `java.toolchain.languageVersion` setting in `build.gradle` to match an available JDK.
- If the H2 console is needed, enable it in `application.yml` and configure the console path.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details. You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.