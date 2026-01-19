package com.example.starlogue.domain.user;

import com.example.starlogue.domain.AbstractEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AbstractEntity {

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "level"))
    private Level level;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "experience_points"))
    private ExperiencePoint exp;

    @Column(nullable = false)
    private Instant createdAt;

    private User(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        this.level = Level.initial();
        this.exp = ExperiencePoint.zero();
        this.createdAt = Instant.now();
    }

    public static User create(String nickname, String email) {
        return new User(nickname, email);
    }

    public void gainExp(int amount) {
        this.exp = this.exp.add(amount);
        checkLevelUp();
    }

    private void checkLevelUp() {
        while (exp.canLevelUp(level)) {
            int previousLevel = level.getValue();
            this.level = level.next();
            // TODO: LevelUpEvent 발행
        }
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
