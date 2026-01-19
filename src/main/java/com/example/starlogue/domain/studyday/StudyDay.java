package com.example.starlogue.domain.studyday;

import com.example.starlogue.domain.AbstractEntity;
import com.example.starlogue.domain.session.BetResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(
        name = "study_days",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}),
        indexes = {
                @Index(name = "idx_study_days_user_date", columnList = "user_id, date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyDay extends AbstractEntity {


    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // === 집계 데이터 ===

    @Column(name = "total_focus_seconds")
    private long totalFocusSeconds;

    @Column(name = "total_sessions")
    private int totalSessions;

    @Column(name = "win_count")
    private int winCount;

    @Column(name = "lose_count")
    private int loseCount;

    // === 별 정보 ===

    @Enumerated(EnumType.STRING)
    @Column(name = "star_type", length = 20)
    private StarType starType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_colors", columnDefinition = "jsonb")
    private Set<String> tagColors;

    // === Streak ===

    @Column(name = "streak_continued")
    private boolean streakContinued;

    @Column(name = "current_streak")
    private int currentStreak;

    // === 하이라이트 ===

    @Embedded
    private DailyHighlight highlight;

    // === Factory ===

    public static StudyDay create(UUID userId, LocalDate date) {
        StudyDay day = new StudyDay();
        day.userId = userId;
        day.date = date;
        day.totalFocusSeconds = 0;
        day.totalSessions = 0;
        day.winCount = 0;
        day.loseCount = 0;
        day.tagColors = new HashSet<>();
        day.streakContinued = false;
        day.currentStreak = 0;
        return day;
    }

    // === Commands ===

    public void addSessionResult(
            BetResult betResult,
            Duration actualFocusTime,
            Set<String> colors
    ) {
        this.totalFocusSeconds += actualFocusTime.toSeconds();
        this.totalSessions++;
        this.tagColors.addAll(colors);

        if (betResult == BetResult.WIN) {
            winCount++;
        } else if (betResult == BetResult.LOSE) {
            loseCount++;
        }

        recalculateStarType();
    }

    private void recalculateStarType() {
        if (loseCount > winCount) {
            this.starType = StarType.BLACKHOLE;
        } else if (getTotalFocusTime().toHours() >= 4 && winCount >= 3) {
            this.starType = StarType.SUPERNOVA;
        } else if (winCount > 0) {
            this.starType = StarType.SHINING_STAR;
        } else {
            this.starType = StarType.METEORITE;
        }
    }

    public void finalizeDay(DailyHighlight highlight, boolean streakContinued, int currentStreak) {
        this.highlight = highlight;
        this.streakContinued = streakContinued;
        this.currentStreak = currentStreak;
    }

    // === Queries ===

    public Duration getTotalFocusTime() {
        return Duration.ofSeconds(totalFocusSeconds);
    }
}