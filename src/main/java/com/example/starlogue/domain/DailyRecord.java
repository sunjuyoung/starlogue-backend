package com.example.starlogue.domain;


import com.example.starlogue.domain.enums.RecordType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 일일 기록
 * 기획서 2-A: 하루가 끝나면 그날의 결과가 우주 지도(별/블랙홀)로 반영
 * 기획서 8: Git 잔디를 넘어서 "공부 은하수"
 */
@Entity
@Table(name = "daily_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "record_date"}),
        indexes = @Index(name = "idx_daily_user_date", columnList = "user_id, record_date DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyRecord extends AbstractEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    // === 기록 유형: 별 or 블랙홀 ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordType recordType;

    // === 별 속성 (recordType이 STAR일 때) ===
    @Column(nullable = false)
    private Integer totalStudyMinutes = 0;  // 총 공부 시간 → 별 크기/밝기

    @Column(length = 7)
    private String primaryColorHex;  // 가장 많이 사용한 태그 색상 → 별 색상

    @Column(nullable = false)
    private Integer brightness = 0;  // 별 밝기 (0~100)

    @Column(nullable = false)
    private Integer size = 0;  // 별 크기 (1~5 단계)

    // === Streak 정보 (연속 성공 시 별 연결) ===
    @Column(nullable = false)
    private Integer streakDay = 0;  // 연속 성공 몇 일차

    @Column(nullable = false)
    private Boolean isStreakStart = false;  // 연속의 시작점

    // === 세션 정보 ===
    @Column(nullable = false)
    private Integer sessionCount = 0;  // 하루 세션 수

    @Column(nullable = false)
    private Integer successSessionCount = 0;  // 성공한 세션 수

    @Column(nullable = false)
    private Integer failedSessionCount = 0;  // 실패한 세션 수

    // === MVP 구간 (초신성 효과) ===
    @Column(nullable = false)
    private Integer maxFocusMinutes = 0;  // 최장 연속 집중 시간

    private String mvpTimeRange;  // "14:10~14:32" 형태

    @Column(nullable = false)
    private Boolean hasSupernova = false;  // 초신성(Nebula) 효과 표시 여부

    // === 연관 관계 ===
    @OneToMany(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudySession> sessions = new ArrayList<>();

    @OneToOne(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private DarkHistory darkHistory;  // 실패 시 생성되는 흑역사

    @OneToOne(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private HighlightReport highlightReport;  // 하루 종료 리포트

    @Builder
    public DailyRecord(User user, LocalDate recordDate) {
        this.user = user;
        this.recordDate = recordDate;
        this.recordType = RecordType.PENDING;  // 초기 상태
    }

    // === 비즈니스 메서드 ===

    /**
     * 세션 결과 반영
     */
    public void addSessionResult(StudySession session) {
        this.sessionCount++;
        this.totalStudyMinutes += session.getTotalStudySeconds() / 60;

        if (session.getIsSuccess()) {
            this.successSessionCount++;
        } else {
            this.failedSessionCount++;
        }

        // 최장 집중 시간 갱신
        int sessionMaxFocus = session.getMaxFocusGauge() / 60;
        if (sessionMaxFocus > this.maxFocusMinutes) {
            this.maxFocusMinutes = sessionMaxFocus;
        }

        // 태그 색상 반영 (가장 많이 사용한 태그)
        if (session.getTag() != null && this.primaryColorHex == null) {
            this.primaryColorHex = session.getTag().getColorHex();
        }

        updateStarProperties();
    }

    /**
     * 별 속성 계산
     */
    private void updateStarProperties() {
        // 밝기: 공부 시간에 비례 (최대 100)
        this.brightness = Math.min(100, this.totalStudyMinutes / 3);  // 300분 = 100%

        // 크기: 1~5 단계
        if (totalStudyMinutes >= 240) this.size = 5;       // 4시간 이상
        else if (totalStudyMinutes >= 180) this.size = 4;  // 3시간 이상
        else if (totalStudyMinutes >= 120) this.size = 3;  // 2시간 이상
        else if (totalStudyMinutes >= 60) this.size = 2;   // 1시간 이상
        else this.size = 1;

        // 초신성 효과: 30분 이상 연속 집중 시
        this.hasSupernova = this.maxFocusMinutes >= 30;
    }

    /**
     * 하루 종료 시 최종 판정
     */
    public void finalize(int currentStreak) {
        // 성공 판정: 하나라도 성공한 세션이 있으면 별
        if (this.successSessionCount > 0) {
            this.recordType = RecordType.STAR;
            this.streakDay = currentStreak;
            this.isStreakStart = currentStreak == 1;
        } else if (this.sessionCount > 0) {
            // 세션은 있지만 모두 실패
            this.recordType = RecordType.BLACK_HOLE;
        } else {
            // 세션 없음 (공부 안 함)
            this.recordType = RecordType.METEORITE;  // 차갑게 식은 운석
        }
    }

    /**
     * 별 여부 확인
     */
    public boolean isStar() {
        return this.recordType == RecordType.STAR;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailed() {
        return this.recordType == RecordType.BLACK_HOLE ||
                this.recordType == RecordType.METEORITE;
    }

    /**
     * 흑역사 연결
     */
    public void attachDarkHistory(DarkHistory darkHistory) {
        this.darkHistory = darkHistory;
    }

    /**
     * 하이라이트 리포트 연결
     */
    public void attachHighlightReport(HighlightReport report) {
        this.highlightReport = report;
    }

    /**
     * MVP 시간대 설정
     */
    public void setMvpTimeRange(String timeRange) {
        this.mvpTimeRange = timeRange;
    }
}