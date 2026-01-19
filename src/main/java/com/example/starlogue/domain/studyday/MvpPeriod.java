package com.example.starlogue.domain.studyday;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record MvpPeriod(
        Instant startTime,
        Instant endTime,
        Duration duration,
        UUID sessionId
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String toDisplayString() {
        return String.format("오늘의 MVP 구간: %s~%s (%d분 무중단)",
                formatTime(startTime),
                formatTime(endTime),
                duration.toMinutes()
        );
    }

    private String formatTime(Instant instant) {
        return TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }
}
