package io.github.tbarland.obscura.migration;

import static org.junit.jupiter.api.Assertions.*;

import io.github.tbarland.obscura.model.Story;
import io.github.tbarland.obscura.repository.StoryRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywayMigrationTests {

  @Autowired private Flyway flyway;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private StoryRepository storyRepository;

  @Test
  void testFlywayMigrationsExecuted() {
    // Arrange & Act
    var migrations = flyway.info().all();

    // Assert - verify at least V1 migration exists and succeeded
    assertTrue(migrations.length > 0, "At least one migration should exist");

    var v1Migration =
        Arrays.stream(migrations)
            .filter(info -> info.getVersion() != null && info.getVersion().getVersion().equals("1"))
            .findFirst();

    assertTrue(v1Migration.isPresent(), "V1 migration should exist");
    assertEquals(
        "Success",
        v1Migration.get().getState().getDisplayName(),
        "V1 migration should have succeeded");
    assertEquals(
        "create story schema",
        v1Migration.get().getDescription(),
        "V1 migration description should match");
  }

  @Test
  void testStoryTableExists() {
    // Arrange & Act
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'STORY'",
            Integer.class);

    // Assert
    assertNotNull(count);
    assertEquals(1, count, "Story table should exist");
  }

  @Test
  void testStoryTagsTableExists() {
    // Arrange & Act
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'STORY_TAGS'",
            Integer.class);

    // Assert
    assertNotNull(count);
    assertEquals(1, count, "Story_tags table should exist");
  }

  @Test
  void testStoryTableHasExpectedColumns() {
    // Arrange - Expected columns
    List<String> expectedColumns = Arrays.asList("ID", "TITLE", "CONTENT", "AUTHOR", "CREATED_AT");

    // Act - Query for actual columns
    List<String> actualColumns =
        jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'STORY'",
            String.class);

    // Assert
    assertTrue(
        actualColumns.containsAll(expectedColumns), "Story table should have all expected columns");
  }

  @Test
  void testStoryTableConstraints() {
    // Act - Query for primary key constraint type (H2 uses TABLE_CONSTRAINTS)
    List<String> constraintTypes =
        jdbcTemplate.queryForList(
            "SELECT CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'STORY'",
            String.class);

    // Assert - verify primary key constraint exists
    assertTrue(
        constraintTypes.stream().anyMatch(t -> t.equals("PRIMARY KEY") || t.equals("PRIMARY_KEY")),
        "Story should have primary key constraint");
  }

  @Test
  @Transactional
  void testCanInsertAndQueryStoryViaJPA() {
    // Arrange
    Story story = new Story();
    story.setTitle("Flyway Test Story");
    story.setContent("Testing that Flyway migrations work correctly with JPA entities");
    story.setAuthor("Integration Test");
    story.setTags(Arrays.asList("test", "flyway", "migration"));
    story.setCreatedAt(LocalDateTime.now());

    // Act
    Story savedStory = storyRepository.save(story);
    Story foundStory = storyRepository.findById(savedStory.getId()).orElse(null);

    // Assert
    assertNotNull(foundStory, "Story should be saved and retrievable");
    assertEquals("Flyway Test Story", foundStory.getTitle());
    assertEquals(
        "Testing that Flyway migrations work correctly with JPA entities", foundStory.getContent());
    assertEquals("Integration Test", foundStory.getAuthor());
    assertEquals(3, foundStory.getTags().size());
    assertTrue(foundStory.getTags().contains("flyway"));

    // Cleanup
    storyRepository.delete(foundStory);
  }

  @Test
  void testStoryTagsForeignKeyConstraint() {
    // Arrange
    Story story = new Story();
    story.setTitle("FK Test Story");
    story.setContent("Testing foreign key constraints");
    story.setAuthor("FK Test");
    story.setTags(Arrays.asList("constraint-test"));
    story.setCreatedAt(LocalDateTime.now());

    // Act
    Story savedStory = storyRepository.save(story);
    Long storyId = savedStory.getId();

    // Assert - verify tags were saved in story_tags table
    Integer tagCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM story_tags WHERE story_id = ?", Integer.class, storyId);

    assertNotNull(tagCount);
    assertEquals(1, tagCount, "Story tags should be stored in story_tags table");

    // Cleanup - deleting story should cascade delete tags due to ON DELETE CASCADE
    storyRepository.delete(savedStory);

    Integer tagsAfterDelete =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM story_tags WHERE story_id = ?", Integer.class, storyId);

    assertEquals(
        0,
        tagsAfterDelete,
        "Tags should be deleted when story is deleted (CASCADE DELETE constraint)");
  }

  @Test
  void testFlywayBaselineConfiguration() {
    // Act
    var config = flyway.getConfiguration();

    // Assert - verify baseline-on-migrate is enabled (from application.yml)
    assertTrue(
        config.isBaselineOnMigrate(),
        "Baseline-on-migrate should be enabled for existing databases");
  }

  @Test
  void testSchemaValidationPasses() {
    // This test verifies that Hibernate's schema validation passes
    // If ddl-auto=validate and the schema doesn't match entities, context loading would fail
    // The fact that @SpringBootTest context loads successfully means validation passed

    // Act - query to ensure we can interact with the database
    long count = storyRepository.count();

    // Assert
    assertTrue(count >= 0, "Should be able to query story count without validation errors");
  }
}
