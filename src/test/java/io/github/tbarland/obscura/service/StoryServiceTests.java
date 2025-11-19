package io.github.tbarland.obscura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tbarland.obscura.dto.StoryRequestDto;
import io.github.tbarland.obscura.model.Story;
import io.github.tbarland.obscura.repository.StoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class StoryServiceTests {

  @InjectMocks private StoryService storyService;

  @Mock private StoryRepository storyRepository;

  @Test
  void testGetAllStories() {

    List<Story> mockStories =
        List.of(
            new Story(
                1L, "Title1", "Content1", "Author1", List.of("tag1", "tag2"), LocalDateTime.now()),
            new Story(2L, "Title2", "Content2", "Author2", List.of("tag3"), LocalDateTime.now()));

    when(storyRepository.findAll()).thenReturn(mockStories);

    var response = storyService.getAllStories();

    assertEquals(2, response.size());
    assertEquals("Title1", response.get(0).title());
    assertEquals("Title2", response.get(1).title());
  }

  @Test
  void testGetStoryById() {
    Long storyId = 1L;
    Story mockStory =
        new Story(
            storyId,
            "Test Title",
            "Test Content",
            "Test Author",
            List.of("tag1", "tag2"),
            LocalDateTime.now());

    when(storyRepository.findById(storyId)).thenReturn(Optional.of(mockStory));

    var response = storyService.getStoryById(storyId);

    assertEquals(storyId, response.id());
    assertEquals("Test Title", response.title());
    assertEquals("Test Content", response.content());
    assertEquals("Test Author", response.author());
    assertEquals(List.of("tag1", "tag2"), response.tags());
  }

  @Test
  void testGetStoryByIdNotFound() {
    Long storyId = 999L;

    when(storyRepository.findById(storyId)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> storyService.getStoryById(storyId));
  }

  @Test
  void testCreateStory() {

    Story mockStory =
        new Story(
            1L, "New Title", "New Content", "New Author", List.of("newtag"), LocalDateTime.now());

    when(storyRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(mockStory);

    var request =
        new io.github.tbarland.obscura.dto.StoryRequestDto(
            "New Title", "New Content", "New Author", List.of("newtag"));

    var response = storyService.createStory(request);

    assertEquals("New Title", response.title());
    assertEquals("New Content", response.content());
    assertEquals("New Author", response.author());
    assertEquals(List.of("newtag"), response.tags());
  }

  @Test
  void testDeleteStory() {
    Long storyId = 1L;

    when(storyRepository.existsById(storyId)).thenReturn(true);

    storyService.deleteStory(storyId);

    verify(storyRepository).deleteById(storyId);
  }

  @Test
  void testDeleteStoryNotFound() {
    Long storyId = 999L;

    when(storyRepository.existsById(storyId)).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> storyService.deleteStory(storyId));
  }

  @Test
  void testUpdateStory() {
    Long storyId = 1L;
    StoryRequestDto mockRequest =
        new StoryRequestDto("Updated Title", "Updated Content", "Updated Author", List.of("tag1"));

    Story existingStory =
        new Story(
            storyId,
            "Old Title",
            "Old Content",
            "Old Author",
            List.of("oldtag"),
            LocalDateTime.now());

    when(storyRepository.findById(storyId)).thenReturn(Optional.of(existingStory));

    var response = storyService.updateStory(storyId, mockRequest);

    assertEquals("Updated Title", response.title());
    assertEquals("Updated Content", response.content());
    assertEquals("Updated Author", response.author());
    assertEquals(List.of("tag1"), response.tags());
  }

  @Test
  void testUpdateStoryNotFound() {
    Long storyId = 999L;
    StoryRequestDto mockRequest =
        new StoryRequestDto("Updated Title", "Updated Content", "Updated Author", List.of("tag1"));

    when(storyRepository.findById(storyId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class, () -> storyService.updateStory(storyId, mockRequest));
  }
}
