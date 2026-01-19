package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StreakBrokenEvent(
        UUID userId,
        int previousStreak,
        Instant occurredAt
) implements DomainEvent {
}
