package io.github.tbarland.obscura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.github.tbarland.obscura.model.Story;
import io.github.tbarland.obscura.repository.StoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class StoryServiceTests {

  @InjectMocks private StoryService storyService;

  @Mock private StoryRepository storyRepository;

  @Test
  public void testGetAllStories() {

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
  public void testCreateStory() {

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
}
