CREATE TABLE story (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE story ADD CONSTRAINT check_title_length 
    CHECK (LENGTH(title) > 0 AND LENGTH(title) <= 100);

ALTER TABLE story ADD CONSTRAINT check_content_not_empty 
    CHECK (LENGTH(content) > 0);

CREATE TABLE story_tags (
    story_id BIGINT NOT NULL,
    tags VARCHAR(255) NOT NULL,
    CONSTRAINT fk_story_tags_story FOREIGN KEY (story_id) 
        REFERENCES story(id) ON DELETE CASCADE
);

ALTER TABLE story_tags ADD CONSTRAINT uk_story_tags 
    UNIQUE (story_id, tags);

CREATE INDEX idx_story_tags_story_id ON story_tags(story_id);

CREATE INDEX idx_story_tags_tags ON story_tags(tags);

CREATE INDEX idx_story_author ON story(author);
CREATE INDEX idx_story_created_at ON story(created_at);