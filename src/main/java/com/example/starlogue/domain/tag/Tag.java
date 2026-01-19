package com.example.starlogue.domain.tag;

import com.example.starlogue.domain.AbstractEntity;
import com.example.starlogue.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Instant createdAt;


    public static Tag create(User user, String name, String colorHex) {
        validateColorHex(colorHex);

        Tag tag = new Tag();
        tag.user = user;

        tag.name = name;
        tag.colorHex = colorHex;
        return tag;
    }

    private static void validateColorHex(String colorHex) {
        if (!colorHex.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("유효하지 않은 색상 코드입니다: " + colorHex);
        }
    }

    public void updateColor(String colorHex) {
        this.colorHex = colorHex;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
