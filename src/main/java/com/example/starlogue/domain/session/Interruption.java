package com.example.starlogue.domain.session;

import com.example.starlogue.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(
        name = "interruptions",
        indexes = {
                @Index(name = "idx_interruptions_session_id", columnList = "session_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interruption extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterruptionReason reason;

    @Column(nullable = false)
    private Instant stoppedAt;

    private Instant resumedAt;

    private Long durationSeconds;

    @Column(nullable = false)
    private int staminaConsumed;

    @Column(nullable = false)
    private int staminaAfter;

    @Column(nullable = false)
    private Instant createdAt;

    private Interruption(Session session, InterruptionReason reason, Instant stoppedAt) {
        this.session = session;
        this.reason = reason;
        this.stoppedAt = stoppedAt;
        this.staminaConsumed = 0;
        this.staminaAfter = 0;
        this.createdAt = Instant.now();
    }

    public static Interruption start(Session session, InterruptionReason reason, Instant stoppedAt) {
        return new Interruption(session, reason, stoppedAt);
    }

    public Duration complete(Instant resumedAt) {
        this.resumedAt = resumedAt;
        Duration duration = Duration.between(stoppedAt, resumedAt);
        this.durationSeconds = duration.toSeconds();
        return duration;
    }

    public void recordStaminaConsumed(int consumed, int after) {
        this.staminaConsumed = consumed;
        this.staminaAfter = after;
    }

    public Duration getDuration() {
        return durationSeconds != null ? Duration.ofSeconds(durationSeconds) : Duration.ZERO;
    }
}
