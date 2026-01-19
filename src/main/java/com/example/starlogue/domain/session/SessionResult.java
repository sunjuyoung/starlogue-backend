package com.example.starlogue.domain.session;

import com.example.starlogue.domain.studyday.StarType;

import java.time.Duration;
import java.util.UUID;

public record SessionResult(
        UUID sessionId,
        BetResult betResult,
        Duration actualFocusTime,
        int finalStaminaPercent,
        int finalGaugePercent,
        Duration longestContinuousFocus,
        int totalExp,
        boolean receivedFocusBonus
) {
    public boolean shouldCreatePenalty() {
        return betResult == BetResult.LOSE;
    }

    public StarType determineStarType() {
        if (betResult == BetResult.LOSE) {
            return StarType.BLACKHOLE;
        }
        if (finalGaugePercent >= 90 && actualFocusTime.toHours() >= 2) {
            return StarType.SUPERNOVA;
        }
        return StarType.SHINING_STAR;
    }
}
