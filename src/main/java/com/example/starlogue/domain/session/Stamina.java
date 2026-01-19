package com.example.starlogue.domain.session;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stamina {

    private static final int MAX = 100;
    private static final int MIN_FOR_WIN = 1;

    private int currentValue;

    private Stamina(int value) {
        this.currentValue = value;
    }

    public static Stamina full() {
        return new Stamina(MAX);
    }

    public int consume(
            InterruptionReason reason,
            Duration interruptionDuration,
            Duration targetSessionDuration
    ) {
        double baseCost = reason.getBaseCostRatio();
        double timeFactor = (double) interruptionDuration.toSeconds()
                / targetSessionDuration.toSeconds();

        int cost = (int) Math.ceil(baseCost * timeFactor * 100);
        int actualCost = Math.min(cost, currentValue);

        this.currentValue = Math.max(0, currentValue - cost);

        return actualCost;
    }

    public boolean canWinBet() {
        return currentValue >= MIN_FOR_WIN;
    }

    public boolean isDepleted() {
        return currentValue <= 0;
    }

    public int getPercentage() {
        return currentValue;
    }
}
