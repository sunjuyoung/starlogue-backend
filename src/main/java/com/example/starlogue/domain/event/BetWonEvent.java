package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BetWonEvent(
        UUID betId,
        UUID sessionId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
}
