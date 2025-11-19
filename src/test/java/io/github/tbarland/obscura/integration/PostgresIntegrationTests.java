package io.github.tbarland.obscura.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.github.tbarland.obscura.dto.StoryRequestDto;
import io.github.tbarland.obscura.dto.StoryResponseDto;
import io.github.tbarland.obscura.model.Story;
import io.github.tbarland.obscura.repository.StoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
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

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private Flyway flyway;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private StoryRepository storyRepository;

  @AfterEach
  void cleanupDatabase() {
    // Clean up all test data after each test to ensure isolation
    storyRepository.deleteAll();
  }

  @Test
  void testPostgresContainerIsRunning() {
    assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    assertEquals("postgres:17-alpine", postgres.getDockerImageName());
  }

  @Test
  void testFlywayMigrationsAppliedToPostgres() {
    // Verify Flyway migrations executed successfully against PostgreSQL
    var migrations = flyway.info().all();

    assertTrue(migrations.length > 0, "At least one migration should exist");

    var v1Migration =
        Arrays.stream(migrations)
            .filter(info -> info.getVersion() != null && info.getVersion().getVersion().equals("1"))
            .findFirst();

    assertTrue(v1Migration.isPresent(), "V1 migration should exist");
    assertEquals(
        "Success",
        v1Migration.get().getState().getDisplayName(),
        "V1 migration should have succeeded on PostgreSQL");
  }

  @Test
  void testPostgresSchemaMatchesProduction() {
    // Verify story table exists in PostgreSQL
    Integer storyTableCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'story'",
            Integer.class);

    assertNotNull(storyTableCount);
    assertEquals(1, storyTableCount, "Story table should exist in PostgreSQL");

    // Verify story_tags table exists
    Integer tagsTableCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'story_tags'",
            Integer.class);

    assertNotNull(tagsTableCount);
    assertEquals(1, tagsTableCount, "Story_tags table should exist in PostgreSQL");

    // Verify expected columns exist
    List<String> expectedColumns = Arrays.asList("id", "title", "content", "author", "created_at");
    List<String> actualColumns =
        jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'story'",
            String.class);

    assertTrue(
        actualColumns.containsAll(expectedColumns),
        "Story table should have all expected columns in PostgreSQL");
  }

  @Test
  void testPostgresConstraintsMatch() {
    // Verify primary key constraint
    List<String> primaryKeys =
        jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE table_schema = 'public' AND table_name = 'story' AND constraint_type = 'PRIMARY KEY'",
            String.class);

    assertFalse(primaryKeys.isEmpty(), "Story table should have primary key in PostgreSQL");

    // Verify check constraints exist
    List<String> checkConstraints =
        jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE table_schema = 'public' AND table_name = 'story' AND constraint_type = 'CHECK'",
            String.class);

    assertTrue(
        checkConstraints.stream().anyMatch(c -> c.contains("title")),
        "Title check constraint should exist");
    assertTrue(
        checkConstraints.stream().anyMatch(c -> c.contains("content")),
        "Content check constraint should exist");

    // Verify foreign key constraint on story_tags
    List<String> foreignKeys =
        jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE table_schema = 'public' AND table_name = 'story_tags' AND constraint_type = 'FOREIGN KEY'",
            String.class);

    assertFalse(foreignKeys.isEmpty(), "Story_tags should have foreign key constraint");
  }

  @Test
  void testPostgresIndexesExist() {
    // Verify indexes created by migration
    List<String> indexes =
        jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename IN ('story', 'story_tags')",
            String.class);

    // Check for expected indexes (names may vary due to auto-generation)
    assertTrue(indexes.size() >= 4, "Should have at least 4 indexes (including primary key)");
  }

  @Test
  void testCreateStoryViaRestApiOnPostgres() {
    // Arrange
    StoryRequestDto request =
        new StoryRequestDto(
            "The Postgres Horror",
            "A tale of transactions and rollbacks in the dark depths of the database...",
            "Integration Test",
            Arrays.asList("postgres", "horror", "integration"));

    // Act
    ResponseEntity<StoryResponseDto> response =
        restTemplate.postForEntity("/api/stories", request, StoryResponseDto.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("The Postgres Horror", response.getBody().title());
    assertEquals(3, response.getBody().tags().size());
    assertTrue(response.getBody().tags().contains("postgres"));

    // Verify in database
    Story savedStory = storyRepository.findById(response.getBody().id()).orElse(null);
    assertNotNull(savedStory, "Story should be persisted in PostgreSQL");
    assertEquals("The Postgres Horror", savedStory.getTitle());
  }

  @Test
  void testGetStoryByIdViaRestApiOnPostgres() {
    // Arrange - create test story
    Story story = new Story();
    story.setTitle("Single Story Test");
    story.setContent("Testing GET by ID endpoint");
    story.setAuthor("Integration Tester");
    story.setTags(Arrays.asList("test", "postgres"));
    story.setCreatedAt(LocalDateTime.now());
    Story savedStory = storyRepository.save(story);

    // Act
    ResponseEntity<StoryResponseDto> response =
        restTemplate.getForEntity("/api/stories/" + savedStory.getId(), StoryResponseDto.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(savedStory.getId(), response.getBody().id());
    assertEquals("Single Story Test", response.getBody().title());
    assertEquals("Testing GET by ID endpoint", response.getBody().content());
    assertEquals("Integration Tester", response.getBody().author());
    assertEquals(2, response.getBody().tags().size());
  }

  @Test
  void testGetStoryByIdNotFoundViaRestApiOnPostgres() {
    // Arrange - use non-existent ID
    Long nonExistentId = 999999L;

    // Act
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/stories/" + nonExistentId, String.class);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testGetAllStoriesViaRestApiOnPostgres() {
    // Arrange - create test data
    Story story1 = new Story();
    story1.setTitle("Container Story 1");
    story1.setContent("A story about containers...");
    story1.setAuthor("Docker");
    story1.setTags(Arrays.asList("container"));
    story1.setCreatedAt(LocalDateTime.now());

    Story story2 = new Story();
    story2.setTitle("Container Story 2");
    story2.setContent("Another container tale...");
    story2.setAuthor("Testcontainers");
    story2.setTags(Arrays.asList("test", "container"));
    story2.setCreatedAt(LocalDateTime.now());

    storyRepository.save(story1);
    storyRepository.save(story2);

    // Act
    ResponseEntity<StoryResponseDto[]> response =
        restTemplate.getForEntity("/api/stories", StoryResponseDto[].class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length >= 2, "Should have at least 2 stories");
  }

  @Test
  void testUpdateStoryViaRestApiOnPostgres() {
    // Arrange - create initial story
    Story story = new Story();
    story.setTitle("Original Title");
    story.setContent("Original content");
    story.setAuthor("Original Author");
    story.setTags(Arrays.asList("original"));
    story.setCreatedAt(LocalDateTime.now());
    Story savedStory = storyRepository.save(story);

    StoryRequestDto updateRequest =
        new StoryRequestDto(
            "Updated Title", "Updated content", "Updated Author", Arrays.asList("updated"));

    // Act
    ResponseEntity<StoryResponseDto> response =
        restTemplate.exchange(
            "/api/stories/" + savedStory.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            StoryResponseDto.class);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Updated Title", response.getBody().title());
    assertEquals("Updated Author", response.getBody().author());

    // Verify in database
    Story updatedStory = storyRepository.findById(savedStory.getId()).orElse(null);
    assertNotNull(updatedStory);
    assertEquals("Updated Title", updatedStory.getTitle());
  }

  @Test
  void testDeleteStoryViaRestApiOnPostgres() {
    // Arrange - create story
    Story story = new Story();
    story.setTitle("To Be Deleted");
    story.setContent("This story will be deleted");
    story.setAuthor("Deletor");
    story.setTags(Arrays.asList("delete"));
    story.setCreatedAt(LocalDateTime.now());
    Story savedStory = storyRepository.save(story);
    Long storyId = savedStory.getId();

    // Act
    ResponseEntity<Void> response =
        restTemplate.exchange("/api/stories/" + storyId, HttpMethod.DELETE, null, Void.class);

    // Assert
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    // Verify deletion in database
    assertFalse(storyRepository.existsById(storyId), "Story should be deleted from PostgreSQL");
  }

  @Test
  void testCascadeDeleteOnPostgres() {
    // Verify that deleting a story cascades to delete tags in PostgreSQL
    Story story = new Story();
    story.setTitle("Cascade Test");
    story.setContent("Testing cascade delete");
    story.setAuthor("Cascade Tester");
    story.setTags(Arrays.asList("cascade", "delete", "test"));
    story.setCreatedAt(LocalDateTime.now());
    Story savedStory = storyRepository.save(story);
    Long storyId = savedStory.getId();

    // Verify tags exist
    Integer tagsBefore =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM story_tags WHERE story_id = ?", Integer.class, storyId);
    assertEquals(3, tagsBefore, "Should have 3 tags before deletion");

    // Delete story
    storyRepository.deleteById(storyId);

    // Verify tags were cascade deleted
    Integer tagsAfter =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM story_tags WHERE story_id = ?", Integer.class, storyId);
    assertEquals(0, tagsAfter, "Tags should be cascade deleted in PostgreSQL");
  }

  @Test
  void testPostgresBigserialIdGeneration() {
    // Verify that PostgreSQL BIGSERIAL auto-generates IDs correctly
    Story story1 = new Story();
    story1.setTitle("ID Test 1");
    story1.setContent("Testing ID generation");
    story1.setAuthor("ID Tester");
    story1.setTags(Arrays.asList("id"));
    story1.setCreatedAt(LocalDateTime.now());

    Story story2 = new Story();
    story2.setTitle("ID Test 2");
    story2.setContent("Testing ID generation");
    story2.setAuthor("ID Tester");
    story2.setTags(Arrays.asList("id"));
    story2.setCreatedAt(LocalDateTime.now());

    Story saved1 = storyRepository.save(story1);
    Story saved2 = storyRepository.save(story2);

    assertNotNull(saved1.getId(), "ID should be auto-generated");
    assertNotNull(saved2.getId(), "ID should be auto-generated");
    assertTrue(saved2.getId() > saved1.getId(), "IDs should be sequential");
  }

  @Test
  void testPostgresTextColumnHandlesLargeContent() {
    // Verify PostgreSQL TEXT column can handle large content (unlike VARCHAR)
    String largeContent = "A".repeat(10000); // 10,000 characters

    Story story = new Story();
    story.setTitle("Large Content Test");
    story.setContent(largeContent);
    story.setAuthor("Content Tester");
    story.setTags(Arrays.asList("large"));
    story.setCreatedAt(LocalDateTime.now());

    Story savedStory = storyRepository.save(story);

    Story retrievedStory = storyRepository.findById(savedStory.getId()).orElse(null);
    assertNotNull(retrievedStory);
    assertEquals(10000, retrievedStory.getContent().length(), "Large content should be stored");
  }

  @Test
  void testValidationWorksWithPostgres() {
    // Verify Spring validation works against PostgreSQL
    StoryRequestDto invalidRequest =
        new StoryRequestDto("", "Content", "Author", Arrays.asList("tag"));

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/stories", invalidRequest, String.class);

    assertEquals(
        HttpStatus.BAD_REQUEST,
        response.getStatusCode(),
        "Empty title should trigger validation error");
  }
}
