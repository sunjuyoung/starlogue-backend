package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record LevelUpEvent(
        UUID userId,
        int previousLevel,
        int newLevel,
        Instant occurredAt
) implements DomainEvent {
}
