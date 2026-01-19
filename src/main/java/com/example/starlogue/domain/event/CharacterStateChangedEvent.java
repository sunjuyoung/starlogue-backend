package com.example.starlogue.domain.event;

import com.example.starlogue.domain.character.CharacterState;

import java.time.Instant;
import java.util.UUID;

public record CharacterStateChangedEvent(
        UUID characterId,
        UUID userId,
        CharacterState previousState,
        CharacterState newState,
        Instant occurredAt
) implements DomainEvent {
}
