package com.example.starlogue.domain;


import com.example.starlogue.domain.enums.ReportTone;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하이라이트 리포트
 * 기획서 9: 하루 종료 리포트: 경기 하이라이트 편집
 * - 스토리형 출력으로 사용자 경험 강화
 * - 숫자보다 스토리로 기억
 */
@Entity
@Table(name = "highlight_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HighlightReport extends AbstractEntity {


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_record_id", nullable = false, unique = true)
    private DailyRecord dailyRecord;

    // === MVP 구간 ===
    @Column(length = 50)
    private String mvpTimeRange;  // "14:10~14:32"

    @Column(nullable = false)
    private Integer mvpDurationMinutes = 0;  // 22분

    @Column(length = 200)
    private String mvpDescription;  // "22분 무중단 집중의 영광"

    // === 위기 순간 ===
    @Column(length = 500)
    private String crisisEvents;  // JSON 또는 구분자로 저장

    @Column(nullable = false)
    private Integer totalCrisisCount = 0;

    // === 전략 제안 (AI 생성) ===
    @Column(length = 500)
    private String strategySuggestion;  // "내일은 시작 10분을 '보스 타임'으로 고정"

    // === 전체 요약 ===
    @Column(length = 1000)
    private String summary;  // AI가 생성한 전체 요약 스토리

    // === 통계 요약 ===
    @Column(nullable = false)
    private Integer totalStudyMinutes = 0;

    @Column(nullable = false)
    private Integer totalPauseMinutes = 0;

    @Column(nullable = false)
    private Integer sessionCount = 0;

    @Column(nullable = false)
    private Double focusRate = 0.0;  // 집중률 (공부시간 / 전체시간)

    // === 감정/톤 ===
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportTone tone;  // 리포트 톤 (축하/격려/냉정)

    @Builder
    public HighlightReport(DailyRecord dailyRecord) {
        this.dailyRecord = dailyRecord;
    }

    // === 비즈니스 메서드 ===

    /**
     * MVP 구간 설정
     */
    public void setMvpSection(String timeRange, int durationMinutes, String description) {
        this.mvpTimeRange = timeRange;
        this.mvpDurationMinutes = durationMinutes;
        this.mvpDescription = description;
    }

    /**
     * 위기 이벤트 추가
     */
    public void addCrisisEvent(String event) {
        if (this.crisisEvents == null || this.crisisEvents.isEmpty()) {
            this.crisisEvents = event;
        } else {
            this.crisisEvents += "|" + event;  // 구분자로 연결
        }
        this.totalCrisisCount++;
    }

    /**
     * 전략 제안 설정 (AI 생성)
     */
    public void setSuggestion(String suggestion) {
        this.strategySuggestion = suggestion;
    }

    /**
     * 요약 설정 (AI 생성)
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 통계 설정
     */
    public void setStatistics(int studyMinutes, int pauseMinutes, int sessions) {
        this.totalStudyMinutes = studyMinutes;
        this.totalPauseMinutes = pauseMinutes;
        this.sessionCount = sessions;

        int totalMinutes = studyMinutes + pauseMinutes;
        this.focusRate = totalMinutes > 0 ? (double) studyMinutes / totalMinutes : 0.0;
    }

    /**
     * 톤 결정
     */
    public void determineTone() {
        if (dailyRecord.isStar() && this.focusRate >= 0.8) {
            this.tone = ReportTone.CELEBRATORY;  // 축하
        } else if (dailyRecord.isStar()) {
            this.tone = ReportTone.ENCOURAGING;  // 격려
        } else {
            this.tone = ReportTone.OBJECTIVE;    // 냉정/객관적
        }
    }

    /**
     * 위기 이벤트 목록 반환
     */
    public String[] getCrisisEventList() {
        if (crisisEvents == null || crisisEvents.isEmpty()) {
            return new String[0];
        }
        return crisisEvents.split("\\|");
    }
}
