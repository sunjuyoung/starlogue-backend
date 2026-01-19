package com.example.starlogue.domain.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionStartedEvent(
        UUID sessionId,
        UUID userId,
        String pledge,
        Duration targetDuration,
        Set<String> tagNames,
        Instant occurredAt
) implements DomainEvent {
}
