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
public class Level {

    private int value;

    public Level(int value) {
        this.value = value;
    }

    public static Level initial() {
        return new Level(1);
    }

    public Level next() {
        return new Level(value + 1);
    }

    public int requiredExp() {
        return value * 100;
    }
}
