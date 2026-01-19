package com.example.starlogue.domain.user;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ExperiencePoint {

    private long value;

    public ExperiencePoint(long value) {
        this.value = value;
    }

    public static ExperiencePoint zero() {
        return new ExperiencePoint(0);
    }

    public ExperiencePoint add(int amount) {
        return new ExperiencePoint(value + amount);
    }

    public boolean canLevelUp(Level currentLevel) {
        return value >= currentLevel.requiredExp();
    }
}
