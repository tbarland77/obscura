package io.github.tbarland.obscura.dto;

import java.util.List;

public record StoryRequestDto(String title, String content, String author, List<String> tags) {}
