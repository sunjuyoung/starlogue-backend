package com.example.starlogue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PenaltyViewedEvent(
        UUID penaltyId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
}
