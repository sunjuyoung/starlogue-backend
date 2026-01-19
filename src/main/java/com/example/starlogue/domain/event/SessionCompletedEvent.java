package com.example.starlogue.domain.event;

import com.example.starlogue.domain.session.BetResult;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record SessionCompletedEvent(
        UUID sessionId,
        UUID userId,
        BetResult betResult,
        Duration actualFocusTime,
        int finalStamina,
        int finalGauge,
        Duration longestContinuousFocus,
        int earnedExp,
        Instant occurredAt
) implements DomainEvent {
}
