package com.example.starlogue.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
