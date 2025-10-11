package io.github.tbarland.obscura.controller;

import io.github.tbarland.obscura.dto.StoryRequestDto;
import io.github.tbarland.obscura.dto.StoryResponseDto;
import io.github.tbarland.obscura.service.StoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

  private final StoryService storyService;

  public StoryController(StoryService storyService) {
    this.storyService = storyService;
  }

  @GetMapping
  public ResponseEntity<List<StoryResponseDto>> getAllStories() {
    return ResponseEntity.ok(storyService.getAllStories());
  }

  @PostMapping()
  public ResponseEntity<StoryResponseDto> createStory(@Valid @RequestBody StoryRequestDto request) {
    return ResponseEntity.ok(storyService.createStory(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<StoryResponseDto> updateStory(
      @PathVariable Long id, @Valid @RequestBody StoryRequestDto request) {
    return ResponseEntity.ok(storyService.updateStory(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
    storyService.deleteStory(id);
    return ResponseEntity.noContent().build();
  }
}
