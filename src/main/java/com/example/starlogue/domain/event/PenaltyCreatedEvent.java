package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PenaltyCreatedEvent(
        UUID penaltyId,
        UUID userId,
        UUID sessionId,
        Instant occurredAt
) implements DomainEvent {
}
