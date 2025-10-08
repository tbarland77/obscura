package io.github.tbarland.obscura.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StoryResponseDto(
    Long id,
    String title,
    String content,
    String author,
    List<String> tags,
    LocalDateTime createdAt) {}
