package com.example.starlogue.domain.penalty;

import com.example.starlogue.domain.session.InterruptionReason;

import java.time.Duration;

public record InterruptionSummary(
        InterruptionReason reason,
        Duration duration,
        int staminaConsumed
) {
}
