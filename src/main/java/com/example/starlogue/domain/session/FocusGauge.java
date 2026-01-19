package com.example.starlogue.domain.session;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusGauge {

    private static final int BONUS_THRESHOLD = 70;

    @Column(name = "target_duration_seconds")
    private long targetDurationSeconds;

    @Column(name = "longest_continuous_focus_seconds")
    private long longestContinuousFocusSeconds;

    private FocusGauge(long targetDurationSeconds) {
        this.targetDurationSeconds = targetDurationSeconds;
        this.longestContinuousFocusSeconds = 0;
    }

    public static FocusGauge create(Duration targetDuration) {
        return new FocusGauge(targetDuration.toSeconds());
    }

    public void recordFocusPeriod(Duration focusDuration) {
        long focusSeconds = focusDuration.toSeconds();
        if (focusSeconds > longestContinuousFocusSeconds) {
            this.longestContinuousFocusSeconds = focusSeconds;
        }
    }

    public int getPercentage() {
        if (targetDurationSeconds == 0) return 0;

        double ratio = (double) longestContinuousFocusSeconds / targetDurationSeconds;
        return (int) Math.min(100, ratio * 100);
    }

    public boolean qualifiesForBonus() {
        return getPercentage() >= BONUS_THRESHOLD;
    }

    public Duration getLongestContinuousFocus() {
        return Duration.ofSeconds(longestContinuousFocusSeconds);
    }

    public Duration getTargetDuration() {
        return Duration.ofSeconds(targetDurationSeconds);
    }
}