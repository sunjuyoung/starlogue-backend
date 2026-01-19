package com.example.starlogue.domain.session;

import com.example.starlogue.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "bets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bet extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(nullable = false)
    private long targetDurationSeconds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pledgeContent;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private BetResult result;

    @Column(length = 100)
    private String failReason;

    private Instant judgedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Bet(Session session, Duration targetDuration, String pledgeContent) {
        this.session = session;
        this.targetDurationSeconds = targetDuration.toSeconds();
        this.pledgeContent = pledgeContent;
        this.result = BetResult.PENDING;
        this.createdAt = Instant.now();
    }

    public static Bet create(Session session, Duration targetDuration, String pledgeContent) {
        return new Bet(session, targetDuration, pledgeContent);
    }

    public void win() {
        this.result = BetResult.WIN;
        this.judgedAt = Instant.now();
    }

    public void lose(String reason) {
        this.result = BetResult.LOSE;
        this.failReason = reason;
        this.judgedAt = Instant.now();
    }

    public Duration getTargetDuration() {
        return Duration.ofSeconds(targetDurationSeconds);
    }
}
