package com.example.starlogue.domain.visualization;

import java.time.Duration;

public record GalaxyStats(
        int totalStars,
        int totalBlackholes,
        int longestStreak,
        Duration totalFocusTime
) {
}
