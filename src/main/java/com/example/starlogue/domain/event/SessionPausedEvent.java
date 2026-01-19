package com.example.starlogue.domain.event;

import com.example.starlogue.domain.session.InterruptionReason;

import java.time.Instant;
import java.util.UUID;

public record SessionPausedEvent(
        UUID sessionId,
        InterruptionReason reason,
        int staminaBefore,
        Instant occurredAt
) implements DomainEvent {
}
