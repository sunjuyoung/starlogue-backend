package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StreakExtendedEvent(
        UUID userId,
        int currentStreak,
        Instant occurredAt
) implements DomainEvent {
}
