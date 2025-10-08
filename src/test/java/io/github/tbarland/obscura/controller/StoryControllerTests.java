package io.github.tbarland.obscura.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.github.tbarland.obscura.dto.StoryRequestDto;
import io.github.tbarland.obscura.dto.StoryResponseDto;
import io.github.tbarland.obscura.service.StoryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StoryControllerTests {

  @InjectMocks private StoryController storyController;

  @Mock private StoryService storyService;

  @Test
  public void testGetAllStories() {
    List<StoryResponseDto> mockStories =
        List.of(
            new StoryResponseDto(
                1L, "Title1", "Content1", "Author1", List.of("tag1", "tag2"), LocalDateTime.now()),
            new StoryResponseDto(
                2L, "Title2", "Content2", "Author2", List.of("tag3"), LocalDateTime.now()));

    when(storyService.getAllStories()).thenReturn(mockStories);

    var response = storyController.getAllStories();

    assertEquals(200, response.getStatusCode().value());
    assertEquals(mockStories, response.getBody());
  }

  @Test
  public void testCreateStory() {

    StoryRequestDto mockRequest =
        new StoryRequestDto("New Title", "New Content", "New Author", List.of("newtag"));

    StoryResponseDto mockStory =
        new StoryResponseDto(
            1L, "New Title", "New Content", "New Author", List.of("newtag"), LocalDateTime.now());

    when(storyService.createStory(mockRequest)).thenReturn(mockStory);

    var response = storyController.createStory(mockRequest);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(mockStory, response.getBody());
  }
}
