package io.github.tbarland.obscura.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryRequestDto(
    @NotBlank(message = "Title must not be blank")
    @Size(max = 100, message = "Title must be at most 100 characters")
    String title,

    @NotBlank(message = "Content must not be blank")
    String content,

    @NotBlank(message = "Author must not be blank")
    String author,
    
    List<String> tags) {
}
