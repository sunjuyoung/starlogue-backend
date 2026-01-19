package com.example.starlogue.domain.event;

import com.example.starlogue.domain.studyday.StarType;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudyDayFinalizedEvent(
        UUID studyDayId,
        UUID userId,
        LocalDate date,
        StarType starType,
        Duration totalFocusTime,
        Instant occurredAt
) implements DomainEvent {
}
