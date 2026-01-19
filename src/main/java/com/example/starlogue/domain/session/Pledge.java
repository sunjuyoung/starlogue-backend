package com.example.starlogue.domain.session;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pledge {

    @Column(name = "pledge_content", nullable = false, length = 500)
    private String content;

    @Column(name = "pledge_created_at")
    private Instant createdAt;

    private Pledge(String content, Instant createdAt) {
        validateContent(content);
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Pledge of(String content) {
        return new Pledge(content, Instant.now());
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("다짐 내용은 필수입니다.");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("다짐은 500자를 초과할 수 없습니다.");
        }
    }
}