package com.example.starlogue.domain;

import com.example.starlogue.domain.enums.SessionStatus;
import com.example.starlogue.domain.enums.StopReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_sessions",
        indexes = {
                @Index(name = "idx_session_user_date", columnList = "user_id, startedAt"),
                @Index(name = "idx_session_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudySession extends AbstractEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;  // 과목/주제 태그 (nullable: 태그 없이도 가능)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_record_id")
    private DailyRecord dailyRecord;  // 일일 기록 (하루 종료 시 연결)

    // === 세션 시간 정보 ===
    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Integer totalStudySeconds = 0;  // 순수 공부 시간 (초)

    @Column(nullable = false)
    private Integer totalPauseSeconds = 0;  // 총 중단 시간 (초)

    // === 게임 시스템: 자원 ===
    @Column(nullable = false)
    private Integer stamina = 100;  // 스태미나 (0~100)

    @Column(nullable = false)
    private Integer focusGauge = 0;  // 현재 연속 집중 시간 (초)

    @Column(nullable = false)
    private Integer maxFocusGauge = 0;  // 세션 내 최장 연속 집중 시간 (초)

    // === 다짐(베팅) ===
    @Embedded
    private Pledge pledge;

    // === 상태 ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(nullable = false)
    private Boolean isSuccess = false;  // 세션 성공 여부

    @Column(nullable = false)
    private Integer brokenPromiseCount = 0;  // 약속 어김 횟수

    // === 중단 이벤트 ===
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stoppedAt ASC")
    private List<StopEvent> stopEvents = new ArrayList<>();

    @Builder
    public StudySession(User user, Tag tag, Pledge pledge) {
        this.user = user;
        this.tag = tag;
        this.pledge = pledge;
        this.startedAt = LocalDateTime.now();
    }

    // === 비즈니스 메서드 ===

    /**
     * 공부 중단 (Stop 버튼)
     */
    public StopEvent stop(StopReason reason, Integer expectedMinutes) {
        // 현재 집중 게이지 저장 후 리셋
        if (this.focusGauge > this.maxFocusGauge) {
            this.maxFocusGauge = this.focusGauge;
        }
        this.focusGauge = 0;

        // 사유별 스태미나 페널티
        int penalty = reason.getStaminaPenalty();
        this.stamina = Math.max(0, this.stamina - penalty);

        // 스태미나 0이면 실패 확정
        if (this.stamina == 0) {
            this.status = SessionStatus.FAILED;
        }

        StopEvent stopEvent = StopEvent.builder()
                .session(this)
                .reason(reason)
                .expectedMinutes(expectedMinutes)
                .build();

        this.stopEvents.add(stopEvent);
        return stopEvent;
    }

    /**
     * 공부 재개 (Resume)
     */
    public void resume(StopEvent stopEvent) {
        stopEvent.resume();

        // 약속 어김 체크 (예상 시간 초과)
        if (stopEvent.getIsBrokenPromise()) {
            this.brokenPromiseCount++;
            // 약속 어김 시 추가 페널티
            this.stamina = Math.max(0, this.stamina - 10);
        }

        // 중단 시간 누적
        this.totalPauseSeconds += stopEvent.getActualPauseSeconds();
    }

    /**
     * 집중 시간 업데이트 (타이머에서 주기적 호출)
     */
    public void updateFocusGauge(int additionalSeconds) {
        this.focusGauge += additionalSeconds;
        this.totalStudySeconds += additionalSeconds;

        // 연속 집중 보상: 세션 시간의 50% 이상 연속 집중 시 스태미나 +10
        int sessionElapsedSeconds = (int) Duration.between(startedAt, LocalDateTime.now()).getSeconds();
        if (this.focusGauge >= sessionElapsedSeconds * 0.5 && this.stamina < 100) {
            this.stamina = Math.min(100, this.stamina + 10);
        }

        if (this.focusGauge > this.maxFocusGauge) {
            this.maxFocusGauge = this.focusGauge;
        }
    }

    /**
     * 세션 종료
     */
    public void end() {
        this.endedAt = LocalDateTime.now();

        // 성공/실패 판정
        if (this.status == SessionStatus.FAILED || this.stamina == 0) {
            this.status = SessionStatus.FAILED;
            this.isSuccess = false;
        } else {
            // 다짐 달성 여부 체크
            this.isSuccess = evaluatePledge();
            this.status = this.isSuccess ? SessionStatus.COMPLETED : SessionStatus.FAILED;
        }
    }

    /**
     * 다짐 달성 여부 평가
     */
    private boolean evaluatePledge() {
        if (pledge == null || pledge.getTargetMinutes() == null) {
            return true;  // 다짐 없으면 성공 처리
        }
        int actualMinutes = this.totalStudySeconds / 60;
        return actualMinutes >= pledge.getTargetMinutes();
    }

    /**
     * 현재 중단 중인지 확인
     */
    public boolean isPaused() {
        if (stopEvents.isEmpty()) return false;
        StopEvent lastStop = stopEvents.get(stopEvents.size() - 1);
        return lastStop.getResumedAt() == null;
    }

    /**
     * 세션 강제 실패 처리
     */
    public void forceFailure() {
        this.status = SessionStatus.FAILED;
        this.isSuccess = false;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 일일 기록에 연결
     */
    public void linkToDailyRecord(DailyRecord dailyRecord) {
        this.dailyRecord = dailyRecord;
    }
}