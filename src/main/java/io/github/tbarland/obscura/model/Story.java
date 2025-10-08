package io.github.tbarland.obscura.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Story {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private String content;
  private String author;
  @ElementCollection private List<String> tags;
  private LocalDateTime createdAt;

  public Story() {}

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Story(
      Long id,
      String title,
      String content,
      String author,
      List<String> tags,
      LocalDateTime createdAt) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.author = author;
    this.tags = tags;
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "Story [id="
        + id
        + ", title="
        + title
        + ", content="
        + content
        + ", author="
        + author
        + ", tags="
        + tags
        + ", createdAt="
        + createdAt
        + "]";
  }
}
