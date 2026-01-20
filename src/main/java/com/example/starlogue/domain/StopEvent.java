package com.example.starlogue.domain;

import com.example.starlogue.domain.enums.StopReason;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 중단 이벤트
 * 기획서 6: Stop(중단) 시스템
 * - Stop 버튼 클릭 시 사유 + 예상 시간 입력
 * - 예상 시간보다 오래 비우면 "약속 어김"
 */
@Entity
@Table(name = "stop_events",
        indexes = @Index(name = "idx_stop_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StopEvent extends AbstractEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StopReason reason;

    @Column(nullable = false)
    private LocalDateTime stoppedAt;

    private LocalDateTime resumedAt;

    @Column(nullable = false)
    private Integer expectedMinutes;  // 예상 중단 시간 (분)

    private Integer actualPauseSeconds;  // 실제 중단 시간 (초)

    @Column(nullable = false)
    private Boolean isBrokenPromise = false;  // 약속 어김 여부

    @Column(length = 100)
    private String note;  // 추가 메모 (선택)

    @Builder
    public StopEvent(StudySession session, StopReason reason, Integer expectedMinutes) {
        this.session = session;
        this.reason = reason;
        this.expectedMinutes = expectedMinutes;
        this.stoppedAt = LocalDateTime.now();
    }

    // === 비즈니스 메서드 ===

    /**
     * 공부 재개
     */
    public void resume() {
        this.resumedAt = LocalDateTime.now();
        this.actualPauseSeconds = (int) Duration.between(stoppedAt, resumedAt).getSeconds();

        // 약속 어김 판정: 예상 시간 초과 시 (기획서: 유예 시간 없음)
        int expectedSeconds = this.expectedMinutes * 60;
        this.isBrokenPromise = this.actualPauseSeconds > expectedSeconds;
    }

    /**
     * 실제 중단 시간 (초)
     * 아직 재개 안했으면 현재까지의 시간 반환
     */
    public int getActualPauseSeconds() {
        if (this.actualPauseSeconds != null) {
            return this.actualPauseSeconds;
        }
        return (int) Duration.between(stoppedAt, LocalDateTime.now()).getSeconds();
    }

    /**
     * 예상 시간 대비 초과 시간 (초)
     */
    public int getOverTimeSeconds() {
        int actual = getActualPauseSeconds();
        int expected = this.expectedMinutes * 60;
        return Math.max(0, actual - expected);
    }

    /**
     * 약속 어김 정도 (0.0 ~ 1.0+)
     * 1.0 = 예상 시간의 2배
     * 흑역사 강도 계산에 사용
     */
    public double getBrokenPromiseSeverity() {
        if (!isBrokenPromise) return 0.0;
        int expected = this.expectedMinutes * 60;
        int overtime = getOverTimeSeconds();
        return (double) overtime / expected;
    }

    /**
     * 메모 추가
     */
    public void addNote(String note) {
        this.note = note;
    }

    /**
     * 아직 재개되지 않았는지 확인
     */
    public boolean isOngoing() {
        return this.resumedAt == null;
    }
}
