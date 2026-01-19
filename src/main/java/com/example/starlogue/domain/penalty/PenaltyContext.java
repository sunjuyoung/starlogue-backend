package com.example.starlogue.domain.penalty;

import java.time.Duration;
import java.util.List;

public record PenaltyContext(
        String originalPledge,
        Duration targetDuration,
        Duration actualDuration,
        int finalStaminaPercent,
        int finalGaugePercent,
        String failReason,
        List<InterruptionSummary> interruptions
) {
}
