package io.github.tbarland.obscura.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

  @Test
  public void testDeleteStory() {
    Long storyId = 1L;

    doNothing().when(storyService).deleteStory(storyId);

    var response = storyController.deleteStory(storyId);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  public void testDeleteStoryNotFound() {
    Long storyId = 999L;

    doThrow(
            new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Story not found with id: " + storyId))
        .when(storyService)
        .deleteStory(storyId);

    assertThrows(ResponseStatusException.class, () -> storyController.deleteStory(storyId));
  }
}
