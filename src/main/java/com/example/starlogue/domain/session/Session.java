package com.example.starlogue.domain.session;

import com.example.starlogue.domain.AbstractEntity;
import com.example.starlogue.domain.studyday.StudyDay;
import com.example.starlogue.domain.tag.Tag;
import com.example.starlogue.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_study_day_id", columnList = "study_day_id"),
                @Index(name = "idx_sessions_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_day_id", nullable = false)
    private StudyDay studyDay;

    @Embedded
    @AttributeOverride(name = "content", column = @Column(name = "pledge_content"))
    @AttributeOverride(name = "createdAt", column = @Column(name = "pledge_created_at"))
    private Pledge pledge;

    @Column(nullable = false)
    private long targetDurationSeconds;

    @ManyToMany
    @JoinTable(
            name = "session_tags",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private Bet bet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    @Embedded
    @AttributeOverride(name = "currentValue", column = @Column(name = "stamina_current"))
    private Stamina stamina;

    @Embedded
    @AttributeOverride(name = "targetSessionDurationSeconds", column = @Column(name = "gauge_target_seconds"))
    @AttributeOverride(name = "longestContinuousFocusSeconds", column = @Column(name = "longest_continuous_focus_seconds"))
    private FocusGauge focusGauge;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interruption> interruptions = new ArrayList<>();

    private Instant currentFocusStartedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Session(User user, StudyDay studyDay, Pledge pledge, Duration targetDuration, Set<Tag> tags) {
        this.user = user;
        this.studyDay = studyDay;
        this.pledge = pledge;
        this.targetDurationSeconds = targetDuration.toSeconds();
        this.tags = new HashSet<>(tags);
        this.status = SessionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.stamina = Stamina.full();
        this.focusGauge = FocusGauge.create(targetDuration);
        this.currentFocusStartedAt = this.startedAt;
        this.createdAt = Instant.now();
    }

    public static Session start(
            User user,
            StudyDay studyDay,
            Pledge pledge,
            Duration targetDuration,
            Set<Tag> tags
    ) {
        Session session = new Session(user, studyDay, pledge, targetDuration, tags);
        session.bet = Bet.create(session, targetDuration, pledge.getContent());
        return session;
    }

    public Interruption pause(InterruptionReason reason) {
        validateActive();

        Instant now = Instant.now();

        Duration continuousFocus = Duration.between(currentFocusStartedAt, now);
        focusGauge.recordFocusPeriod(continuousFocus);

        this.status = SessionStatus.PAUSED;

        return Interruption.start(this, reason, now);
    }

    public void resume(Interruption interruption) {
        validatePaused();

        Instant now = Instant.now();
        Duration interruptionDuration = interruption.complete(now);

        int consumed = stamina.consume(
                interruption.getReason(),
                interruptionDuration,
                getTargetDuration()
        );

        interruption.recordStaminaConsumed(consumed, stamina.getPercentage());
        this.interruptions.add(interruption);

        this.currentFocusStartedAt = now;
        this.status = SessionStatus.ACTIVE;
    }

    public SessionResult complete() {
        Instant now = Instant.now();

        if (status == SessionStatus.ACTIVE) {
            Duration finalFocus = Duration.between(currentFocusStartedAt, now);
            focusGauge.recordFocusPeriod(finalFocus);
        }

        this.endedAt = now;
        this.status = SessionStatus.COMPLETED;

        Duration actualFocusTime = calculateActualFocusTime();
        BetResult betResult = judgeBet(actualFocusTime);

        int baseExp = (int) actualFocusTime.toMinutes();
        int winBonus = betResult == BetResult.WIN ? 100 : 0;
        int focusBonus = focusGauge.qualifiesForBonus() ? 50 : 0;

        return new SessionResult(
                this.getId(),
                betResult,
                actualFocusTime,
                stamina.getPercentage(),
                focusGauge.getPercentage(),
                focusGauge.getLongestContinuousFocus(),
                baseExp + winBonus + focusBonus,
                focusBonus > 0
        );
    }

    public SessionResult abandon() {
        Instant now = Instant.now();

        if (status == SessionStatus.ACTIVE) {
            Duration finalFocus = Duration.between(currentFocusStartedAt, now);
            focusGauge.recordFocusPeriod(finalFocus);
        }

        this.endedAt = now;
        this.status = SessionStatus.ABANDONED;
        this.bet.lose("세션 포기");

        Duration actualFocusTime = calculateActualFocusTime();
        int baseExp = (int) (actualFocusTime.toMinutes() * 0.5);

        return new SessionResult(
                this.getId(),
                BetResult.LOSE,
                actualFocusTime,
                stamina.getPercentage(),
                focusGauge.getPercentage(),
                focusGauge.getLongestContinuousFocus(),
                baseExp,
                false
        );
    }

    private BetResult judgeBet(Duration actualFocusTime) {
        boolean timeAchieved = actualFocusTime.compareTo(getTargetDuration()) >= 0;
        boolean staminaSufficient = stamina.canWinBet();

        if (timeAchieved && staminaSufficient) {
            bet.win();
            return BetResult.WIN;
        } else {
            String reason = !staminaSufficient ? "스태미나 고갈" : "목표 시간 미달";
            bet.lose(reason);
            return BetResult.LOSE;
        }
    }

    private Duration calculateActualFocusTime() {
        Duration totalInterruption = interruptions.stream()
                .map(Interruption::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        Duration totalElapsed = Duration.between(startedAt, endedAt);
        return totalElapsed.minus(totalInterruption);
    }

    private void validateActive() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태에서만 중단할 수 있습니다.");
        }
    }

    private void validatePaused() {
        if (status != SessionStatus.PAUSED) {
            throw new IllegalStateException("일시정지 상태에서만 재개할 수 있습니다.");
        }
    }

    public Duration getTargetDuration() {
        return Duration.ofSeconds(targetDurationSeconds);
    }
}
