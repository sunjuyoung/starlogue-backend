package com.example.starlogue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String name;  // 예: "수학", "영어", "코딩"

    @Column(nullable = false, length = 7)
    private String colorHex;  // 별 색상 (예: "#FF5733")

    @Column(length = 50)
    private String icon;  // 아이콘 이름 또는 이모지

    @Column(nullable = false)
    private Integer usageCount = 0;  // 사용 횟수 (인기 태그 정렬용)

    @Column(nullable = false)
    private Boolean isActive = true;  // 삭제 대신 비활성화

    @Builder
    public Tag(User user, String name, String colorHex, String icon) {
        this.user = user;
        this.name = name;
        this.colorHex = colorHex;
        this.icon = icon;
    }

    // === 비즈니스 메서드 ===

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void update(String name, String colorHex, String icon) {
        this.name = name;
        this.colorHex = colorHex;
        this.icon = icon;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}