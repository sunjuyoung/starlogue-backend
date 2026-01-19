package com.example.starlogue.domain.studyday;

import com.example.starlogue.domain.session.InterruptionReason;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CrisisEvent(
        Instant stoppedAt,
        Instant resumedAt,
        InterruptionReason reason,
        UUID sessionId
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String toDisplayString() {
        return String.format("위기 순간: %s Stop → %s Resume (%s)",
                formatTime(stoppedAt),
                formatTime(resumedAt),
                reason.getDisplayName()
        );
    }

    private String formatTime(Instant instant) {
        return TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }
}
