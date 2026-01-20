# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Obscura is a Spring Boot 3.5.6 REST API demonstrating clean architecture with a layered controller-service-repository pattern. The application manages "stories" (with a horror/suspense theme) through CRUD endpoints, using Spring Data JPA with Flyway migrations, Jakarta Bean Validation, and comprehensive test coverage. Supports multiple database profiles: H2 (local/test) and PostgreSQL (production).

## Essential Commands

### Running the Application

```bash
# Local development with H2 (default profile - fastest iteration)
./gradlew bootRun

# Local development with specific profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Test profile (H2 with clean output)
./gradlew bootRun --args='--spring.profiles.active=test'

# Production profile (PostgreSQL - requires DB_PASSWORD env var)
export DB_PASSWORD=yourpassword
./gradlew bootRun --args='--spring.profiles.active=prod'

# Docker Compose with H2 (uses override for dev mode with hot reload)
docker-compose up

# Docker Compose with PostgreSQL (production-like, matches production DB)
docker-compose -f docker-compose.postgres.yml up

# Docker Compose with PostgreSQL (development mode with hot reload)
docker-compose -f docker-compose.postgres-dev.yml up

# Production-like Docker build
docker build -t obscura:latest .
docker run -p 8080:8080 --rm obscura:latest
```

### Testing and Quality

```bash
# Run all tests (requires Docker for Testcontainers tests)
./gradlew test

# Run all tests excluding PostgreSQL integration tests (no Docker required)
./gradlew test --tests '*' -Dtest.excludeTags=integration

# Run single test class
./gradlew test --tests StoryServiceTests

# Run specific test method
./gradlew test --tests StoryServiceTests.testCreateStory

# Run PostgreSQL integration tests only (requires Docker)
./gradlew test -Dtest.includeTags=integration

# Full build with coverage verification (85% minimum enforced)
./gradlew clean build

# View coverage report (after running tests)
open build/reports/jacoco/test/html/index.html
```

### Code Formatting

```bash
# Check formatting (CI runs this)
./gradlew spotlessCheck

# Auto-format all code (run before commits)
./gradlew spotlessApply
```

## Architecture

### Layered Design Pattern

The codebase follows strict three-tier architecture with unidirectional dependencies:

```
Controller (REST) → Service (Business Logic) → Repository (Data Access) → Database (H2/PostgreSQL)
                                                                           ↓
                                                                    Flyway Migrations
```

**Critical architectural rules:**
- Controllers only call services, never repositories
- Services contain all business logic and transaction boundaries
- Repositories extend JpaRepository (no custom implementations currently)
- DTOs separate API contracts from persistence entities

### Key Components

**Controllers** (`controller/StoryController.java`):
- Use constructor injection (never field injection)
- Annotate request DTOs with `@Valid` for automatic validation
- Return `ResponseEntity<T>` for explicit HTTP status codes
- DELETE returns `204 No Content`, all others return `200 OK`

**Services** (`service/StoryService.java`):
- Class-level `@Transactional(readOnly = true)` for optimization
- Write operations override with `@Transactional` (no readOnly)
- Throw `ResponseStatusException` for errors (auto-converts to HTTP responses)
- Manual DTO-to-Entity mapping (no MapStruct currently)

**Repositories** (`repository/StoryRepository.java`):
- Extend `JpaRepository<Story, Long>` for automatic CRUD
- No custom query methods needed yet
- Follow naming convention: `{Entity}Repository`

**DTOs** (`dto/`):
- Use Java records (immutable, auto-generates equals/hashCode/toString)
- `StoryRequestDto`: Input validation with `@NotBlank`, `@Size`
- `StoryResponseDto`: Output with all fields including `id` and `createdAt`

**Entities** (`model/Story.java`):
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` for auto-increment IDs
- `@Column` annotations match Flyway migration schema constraints
- `@ElementCollection` for `List<String> tags` (creates join table)
- Both no-arg (JPA requirement) and full-arg constructors
- `LocalDateTime` fields auto-map to TIMESTAMP

### Transaction Management

**Default behavior:**
```java
@Service
@Transactional(readOnly = true)  // Optimizes reads, prevents accidental writes
public class StoryService {

    @Transactional  // Override for writes
    public StoryResponseDto createStory(StoryRequestDto dto) { ... }
}
```

### Error Handling Strategy

**Always use ResponseStatusException** in services:
```java
if (!storyRepository.existsById(id)) {
    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Story not found with id: " + id
    );
}
```

Spring automatically converts this to proper REST error response with timestamp, status, error, message, and path.

## Testing Patterns

### Unit Test Structure (AAA Pattern)

All tests follow Arrange-Act-Assert with JUnit 5 conventions (package-private classes and methods):

```java
@ExtendWith(MockitoExtension.class)  // JUnit 5 style
class StoryServiceTests {  // Package-private (no public modifier)

    @InjectMocks
    private StoryService storyService;

    @Mock
    private StoryRepository storyRepository;

    @Test
    void testCreateStory() {  // Package-private (no public modifier)
        // Arrange - set up test data and mock behavior
        Story mockStory = new Story(...);
        when(storyRepository.save(any())).thenReturn(mockStory);

        // Act - call method under test
        var response = storyService.createStory(request);

        // Assert - verify results
        assertEquals("Title", response.title());
    }
}
```

### Mocking Patterns

**Service tests**: Mock `StoryRepository`
- Use `when(...).thenReturn(...)` for stubbing
- Use `ArgumentMatchers.any()` for flexible matching

**Controller tests**: Mock `StoryService`
- Use `doThrow(...).when(...)` for void methods that throw exceptions
- Test `ResponseEntity` status codes and bodies

### Coverage Requirements

- **Minimum 85% line coverage** enforced by JaCoCo
- Build fails if coverage drops below threshold
- Reports generated at `build/reports/jacoco/test/html/`
- CI uploads coverage reports as artifacts

### Integration Tests for Flyway Migrations

**Location**: `test/java/.../migration/FlywayMigrationTests.java`

Comprehensive integration tests verify:
1. Flyway migrations execute successfully
2. Database tables and columns exist as expected
3. Constraints (primary keys, foreign keys, unique) are created
4. JPA entities work correctly with migrated schema
5. Cascade delete behaviors function properly
6. Hibernate's `ddl-auto: validate` passes

**Example test structure:**
```java
@SpringBootTest
class FlywayMigrationTests {
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StoryRepository storyRepository;

    @Test
    void testFlywayMigrationsExecuted() {
        var migrations = flyway.info().all();
        // Verify V1 migration succeeded
    }

    @Test
    @Transactional
    void testCanInsertAndQueryStoryViaJPA() {
        // Test full JPA CRUD with migrated schema
    }
}
```

### Production-Like Integration Tests with Testcontainers

**Location**: `test/java/.../integration/PostgresIntegrationTests.java`

For true production parity, the project includes Testcontainers-based integration tests that spin up a real PostgreSQL container. These tests verify the application works identically on PostgreSQL as it does on H2.

**Requirements:**
- Docker installed and running
- Sufficient disk space for PostgreSQL image (~50MB)
- Network access to pull Docker images

**Key Features:**
- Uses `postgres:17-alpine` matching production version
- Tests full REST API against real PostgreSQL database
- Verifies Flyway migrations work on PostgreSQL
- Validates PostgreSQL-specific features (BIGSERIAL, TEXT columns, CASCADE DELETE)
- Tests run in complete isolation (container lifecycle managed by Testcontainers)

**Example structure:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PostgresIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("obscura_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testCreateStoryViaRestApiOnPostgres() {
        // Full end-to-end test against real PostgreSQL
    }
}
```

**Running Testcontainers tests:**
```bash
# Ensure Docker is running first
docker ps

# Run all tests (includes Testcontainers if Docker available)
./gradlew test

# Run only PostgreSQL integration tests
./gradlew test -Dtest.includeTags=integration

# Skip Testcontainers tests (if Docker not available)
./gradlew test -x PostgresIntegrationTests
```

**What these tests verify:**
1. PostgreSQL container starts successfully
2. Flyway migrations execute on PostgreSQL (not just H2)
3. Schema matches production (tables, columns, constraints, indexes)
4. BIGSERIAL auto-increment works correctly
5. TEXT columns handle large content (10,000+ characters)
6. CASCADE DELETE works on PostgreSQL
7. Full REST API CRUD operations work end-to-end
8. Spring validation integrates with PostgreSQL
9. Foreign key constraints are enforced
10. Unique constraints work correctly

**CI/CD Configuration:**
- **Separate CI Job**: PostgreSQL integration tests run in dedicated `integration-tests` job
- **Only on PRs and main**: Tests run on pull requests to main and pushes to main (not on every commit to feature branches)
- **After main build**: Integration tests only run if main build (unit tests + Flyway tests) passes first
- **Docker pre-installed**: GitHub Actions runners have Docker available by default
- **Faster feedback**: Unit tests (~10s) run first, PostgreSQL tests (~30s with container startup) run after

**Local Development:**
- If Docker not running, these tests will be skipped automatically
- Fast iteration uses H2-based tests (`FlywayMigrationTests`)
- Run Testcontainers tests before raising PRs to catch PostgreSQL-specific issues

## Code Quality Enforcement

### Spotless (Google Java Format)

**Pre-commit workflow:**
1. Make code changes
2. Run `./gradlew spotlessApply` to auto-format
3. Commit changes

**CI enforces formatting** - build fails if code not formatted with Google Java Format (2-space indentation).

### Build Quality Gates

The build pipeline enforces:
1. Code formatting (Spotless)
2. All tests pass
3. 85% minimum line coverage (JaCoCo)
4. No unused imports

All enforced via `./gradlew check` (which runs on every build).

## Development Workflow

### Docker Compose Files

The project provides multiple Docker Compose configurations for different use cases:

#### H2 Development (Default - Fastest Iteration)
- **docker-compose.yml**: Base config with H2 database
- **docker-compose.override.yml**: Development overrides with:
  - Live code mounting (`./:/workspace:cached`)
  - Gradle cache persistence
  - Spring DevTools enabled
  - No restart on failure

Running `docker-compose up` automatically merges both files for optimal local development with H2.

#### PostgreSQL (Production-like Testing)
- **docker-compose.postgres.yml**: Production-like setup
  - PostgreSQL 17 container (matches production version)
  - Uses production Spring profile
  - Health checks ensure DB is ready before app starts
  - Data persisted in Docker volume `postgres-data`
  - Best for final testing before deployment

- **docker-compose.postgres-dev.yml**: Development with PostgreSQL
  - PostgreSQL 17 container
  - Live code mounting for hot reload
  - Gradle cache persistence
  - Spring DevTools enabled
  - Best for testing Flyway migrations during development

**When to use each:**
- `docker-compose up` - Fast iteration with H2 (no DB persistence)
- `docker-compose -f docker-compose.postgres-dev.yml up` - Test migrations with hot reload
- `docker-compose -f docker-compose.postgres.yml up` - Final verification before PR

### Hot Reload

Spring DevTools is configured for fast feedback:
- Watches for file changes
- Restarts only application code (not JVM)
- Works with both `./gradlew bootRun` and `docker-compose up`

### Java Toolchain

Project requires Java 21 (configured in build.gradle):
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Gradle will auto-download JDK 21 if not available locally.

## Database

### Multi-Database Profile Strategy

The application uses Spring profiles to support different database configurations:

**Available Profiles:**
- **default** - H2 in-memory (no profile specified)
- **local** (`application-local.yml`) - H2 with console enabled, SQL logging
- **test** (`application-test.yml`) - H2 for testing with clean output
- **prod** (`application-prod.yml`) - PostgreSQL for production

**Profile Configuration Files:**
- `application.yml` - Base configuration (Flyway, JPA validation)
- `application-local.yml` - H2 with dev tools
- `application-test.yml` - H2 optimized for tests
- `application-prod.yml` - PostgreSQL with environment variables

### Flyway Database Migrations

**Migration Strategy:**
- Flyway owns schema creation and evolution
- Hibernate validates schema with `ddl-auto: validate`
- Migrations are versioned SQL scripts in `src/main/resources/db/migration/`

**Migration Naming Convention:**
```
V{version}__{description}.sql

Examples:
V1__create_story_schema.sql
V2__add_story_rating_column.sql
```

**Key Configuration (`application.yml`):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema, Hibernate validates
  flyway:
    enabled: true
    baseline-on-migrate: true  # Handles existing databases
    locations: classpath:db/migration
```

**Important Rules:**
1. Never modify existing migrations - create new ones
2. Always include rollback strategy in migration comments
3. Test migrations against PostgreSQL before production
4. Ensure entity `@Column` annotations match migration schema

### H2 Configuration (Local/Test)

- **In-memory database**: Data resets on application restart
- **H2 Console enabled**: Access at `http://localhost:8080/h2-console` (local profile)
- **JDBC URL**: `jdbc:h2:mem:obscura` (local) or `jdbc:h2:mem:testdb` (test)
- **Credentials**: username=`sa`, password=(empty)

### PostgreSQL Configuration (Production)

**Environment Variables Required:**
```bash
DB_HOST=localhost          # Default if not set
DB_PORT=5432              # Default if not set
DB_NAME=obscura           # Default if not set
DB_USERNAME=obscura       # Default if not set
DB_PASSWORD=<required>    # Must be provided
```

**Connection String:**
```
jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

### Entity Mapping

**@Column annotations must match Flyway migrations:**
```java
@Entity
public class Story {
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    private List<String> tags;  // Creates story_tags join table
}
```

Flyway creates:
- Main table: `story` with constraints
- Join table: `story_tags` with foreign key CASCADE DELETE

## API Design

### Validation Pattern

**Input validation at boundary:**
```java
// DTO
public record StoryRequestDto(
    @NotBlank(message = "Title must not be blank")
    @Size(max = 100, message = "Title must be at most 100 characters")
    String title,
    @NotBlank(message = "Content must not be blank")
    String content,
    @NotBlank(message = "Author must not be blank")
    String author,
    List<String> tags
) {}

// Controller
@PostMapping()
public ResponseEntity<StoryResponseDto> createStory(
    @Valid @RequestBody StoryRequestDto request  // @Valid triggers validation
) {
    return ResponseEntity.ok(storyService.createStory(request));
}
```

Spring auto-returns 400 Bad Request with field-level error details for validation failures.

### Endpoints

Base URL: `/api/stories`

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/api/stories` | 200 | Returns all stories |
| GET | `/api/stories/{id}` | 200 | Returns single story, 404 if not found |
| POST | `/api/stories` | 200 | Creates story, validates input |
| PUT | `/api/stories/{id}` | 200 | Updates existing story |
| DELETE | `/api/stories/{id}` | 204 | No Content on success, 404 if not found |

### Actuator Endpoints

Enabled endpoints (configured in application.yml):
- `/actuator/health` - Health check (UP/DOWN)
- `/actuator/info` - Application metadata
- `/actuator/metrics` - JVM and HTTP metrics

### CORS Configuration

**Location:** `src/main/java/io/github/tbarland/obscura/config/WebConfig.java`

The API enables CORS for frontend development:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
```

**Allowed origins:**
- `http://localhost:5173` - Vite default dev server
- `http://localhost:3000` - React (Create React App) dev server
- `http://127.0.0.1:5173` - Alternative localhost address

**Configuration notes:**
- Applies to all `/api/**` endpoints
- Credentials enabled for future authentication support
- Preflight requests cached for 1 hour (3600 seconds)
- For production, externalize origins to environment-specific application properties

**Testing:** See `WebConfigTests.java` for comprehensive CORS test coverage.

## Adding New Features

### Adding a New Entity (e.g., Author)

1. **Create Flyway Migration** (`src/main/resources/db/migration/V2__create_author_table.sql`):
   ```sql
   CREATE TABLE author (
       id BIGSERIAL PRIMARY KEY,
       name VARCHAR(100) NOT NULL,
       created_at TIMESTAMP NOT NULL
   );
   ```

2. **Create Entity** (`model/Author.java`):
   ```java
   @Entity
   public class Author {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(nullable = false, length = 100)
       private String name;

       @Column(nullable = false, updatable = false)
       private LocalDateTime createdAt;
       // constructors, getters/setters
   }
   ```

3. **Create DTOs** (`dto/AuthorRequestDto.java`, `dto/AuthorResponseDto.java`):
   ```java
   public record AuthorRequestDto(
       @NotBlank String name
   ) {}
   ```

4. **Create Repository** (`repository/AuthorRepository.java`):
   ```java
   @Repository
   public interface AuthorRepository extends JpaRepository<Author, Long> {}
   ```

5. **Create Service** (`service/AuthorService.java`):
   ```java
   @Service
   @Transactional(readOnly = true)
   public class AuthorService {
       private final AuthorRepository authorRepository;
       // methods with @Transactional for writes
   }
   ```

6. **Create Controller** (`controller/AuthorController.java`):
   ```java
   @RestController
   @RequestMapping("/api/authors")
   public class AuthorController {
       private final AuthorService authorService;
       // endpoints
   }
   ```

7. **Create Tests**:
   - `service/AuthorServiceTests.java` - mock repository (package-private class/methods)
   - `controller/AuthorControllerTests.java` - mock service (package-private class/methods)
   - `migration/FlywayAuthorMigrationTests.java` - verify migration and schema

8. **Run quality checks**:
   ```bash
   ./gradlew spotlessApply
   ./gradlew clean build  # Ensures tests pass and coverage maintained
   ```

### Adding Custom Repository Methods

If you need custom queries, use Spring Data JPA method naming:

```java
@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByAuthor(String author);  // Derived query
    List<Story> findByCreatedAtAfter(LocalDateTime date);  // Derived query

    @Query("SELECT s FROM Story s WHERE s.author = :author AND SIZE(s.tags) > 0")
    List<Story> findStoriesWithTagsByAuthor(@Param("author") String author);  // Custom JPQL
}
```

Spring Data auto-implements methods based on naming conventions.

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`):

1. **Checkout** code
2. **Cache** Gradle dependencies
3. **Setup** JDK 21
4. **Verify formatting** (`spotlessCheck`)
5. **Build** with tests (`./gradlew clean build`)
6. **Upload** JaCoCo coverage reports as artifacts

**Pipeline fails if:**
- Code not formatted with Google Java Format
- Any test fails
- Coverage drops below 85%

## Common Pitfalls

### Don't Create Commits Without Formatting
Always run `./gradlew spotlessApply` before committing. CI will fail on unformatted code.

### Don't Modify Existing Flyway Migrations
Never edit existing migration files. Always create new versioned migrations for schema changes.

### Don't Let Entity Schema Drift from Migrations
Entity `@Column` annotations must match Flyway migration schema. Use `ddl-auto: validate` to catch mismatches.

### Don't Skip Transaction Annotations
Write operations in services must have `@Transactional`. Read operations benefit from class-level `@Transactional(readOnly = true)`.

### Don't Use Field Injection
Always use constructor injection for dependencies:
```java
// Good
public StoryService(StoryRepository storyRepository) {
    this.storyRepository = storyRepository;
}

// Bad - don't do this
@Autowired
private StoryRepository storyRepository;
```

### Don't Make Test Classes/Methods Public
JUnit 5 doesn't require `public` - use package-private (no modifier):
```java
// Good
class StoryServiceTests {
    @Test
    void testCreateStory() { ... }
}

// Bad - unnecessary public modifiers
public class StoryServiceTests {
    @Test
    public void testCreateStory() { ... }
}
```

### Don't Return Entities from Controllers
Always use DTOs to separate persistence from API contracts:
```java
// Good
public ResponseEntity<StoryResponseDto> getStory(Long id) { ... }

// Bad - don't expose entities directly
public ResponseEntity<Story> getStory(Long id) { ... }
```

### Don't Forget @Valid
Controller methods accepting request bodies must use `@Valid`:
```java
@PostMapping()
public ResponseEntity<StoryResponseDto> createStory(
    @Valid @RequestBody StoryRequestDto request  // Required for validation
) { ... }
```

## Project Metadata

- **Group**: io.github.tbarland
- **Version**: 0.0.1-SNAPSHOT
- **Java**: 21 (toolchain enforced)
- **Spring Boot**: 3.5.6
- **Gradle**: 8.14.3
- **Main Class**: `io.github.tbarland.obscura.ObscuraApplication`
