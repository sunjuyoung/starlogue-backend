package com.example.starlogue.domain.session;

import lombok.Getter;

@Getter
public enum InterruptionReason {
    TOILET(0.05, "화장실"),
    REST(0.10, "휴식"),
    INTERFERENCE(0.15, "외부 방해"),
    DISTRACTION(0.25, "딴짓");

    private final double baseCostRatio;
    private final String displayName;

    InterruptionReason(double baseCostRatio, String displayName) {
        this.baseCostRatio = baseCostRatio;
        this.displayName = displayName;
    }
}
