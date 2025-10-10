package io.github.tbarland.obscura.service;

import io.github.tbarland.obscura.dto.StoryRequestDto;
import io.github.tbarland.obscura.dto.StoryResponseDto;
import io.github.tbarland.obscura.model.Story;
import io.github.tbarland.obscura.repository.StoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoryService {
  private final StoryRepository storyRepository;

  public StoryService(StoryRepository storyRepository) {
    this.storyRepository = storyRepository;
  }

  public List<StoryResponseDto> getAllStories() {
    return storyRepository.findAll().stream()
        .map(
            story ->
                new StoryResponseDto(
                    story.getId(),
                    story.getTitle(),
                    story.getContent(),
                    story.getAuthor(),
                    story.getTags(),
                    story.getCreatedAt()))
        .toList();
  }

  public StoryResponseDto createStory(StoryRequestDto dto) {
    Story story = new Story();
    story.setTitle(dto.title());
    story.setContent(dto.content());
    story.setAuthor(dto.author());
    story.setTags(dto.tags());
    story.setCreatedAt(LocalDateTime.now());

    Story saved = storyRepository.save(story);

    return new StoryResponseDto(
        saved.getId(),
        saved.getTitle(),
        saved.getContent(),
        saved.getAuthor(),
        saved.getTags(),
        saved.getCreatedAt());
  }

  public void deleteStory(Long id) {
    if (!storyRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found with id: " + id);
    }
    storyRepository.deleteById(id);
  }
}
