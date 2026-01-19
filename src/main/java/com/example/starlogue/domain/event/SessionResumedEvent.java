package com.example.starlogue.domain.event;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record SessionResumedEvent(
        UUID sessionId,
        Duration interruptionDuration,
        int staminaConsumed,
        int staminaAfter,
        Instant occurredAt
) implements DomainEvent {
}
