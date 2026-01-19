package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BetLostEvent(
        UUID betId,
        UUID sessionId,
        UUID userId,
        String failReason,
        Instant occurredAt
) implements DomainEvent {
}
