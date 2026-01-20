package com.example.starlogue.domain;


import com.example.starlogue.domain.enums.SatireLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 흑역사 (Dark History)
 * 기획서 3-B: 목표 달성 실패 시 AI가 '흑역사'(풍자/블랙코미디 톤)를 생성
 * - 분량: 30~180자 짧은 박제 (부담 최소화)
 * - 블랙홀 클릭 시 팝업으로 노출
 */
@Entity
@Table(name = "dark_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DarkHistory extends AbstractEntity {



    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_record_id", nullable = false, unique = true)
    private DailyRecord dailyRecord;

    // === 흑역사 콘텐츠 ===
    @Column(nullable = false, length = 200)
    private String content;  // AI가 생성한 흑역사 (30~180자)

    @Column(length = 50)
    private String title;  // 제목 (예: "나약한 인간의 일기 #42")

    // === 생성 기반 정보 ===
    @Column(length = 200)
    private String originalPledge;  // 원래 다짐 (흑역사 생성의 재료)

    @Column(nullable = false)
    private Integer failedMinutes = 0;  // 실패까지 공부한 시간

    @Column(nullable = false)
    private Integer brokenPromiseCount = 0;  // 약속 어김 횟수

    // === 풍자 레벨 ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SatireLevel satireLevel;

    // === 조회/공개 설정 ===
    @Column(nullable = false)
    private Integer viewCount = 0;  // 조회 수

    @Column(nullable = false)
    private Boolean isPublic = false;  // 공개 여부 (다른 사용자에게)

    @Column(nullable = false)
    private Boolean isAcknowledged = false;  // 사용자가 확인했는지

    @Builder
    public DarkHistory(DailyRecord dailyRecord, String content, String title,
                       String originalPledge, int failedMinutes,
                       int brokenPromiseCount, SatireLevel satireLevel) {
        this.dailyRecord = dailyRecord;
        this.content = content;
        this.title = title;
        this.originalPledge = originalPledge;
        this.failedMinutes = failedMinutes;
        this.brokenPromiseCount = brokenPromiseCount;
        this.satireLevel = satireLevel;
    }

    // === 비즈니스 메서드 ===

    /**
     * 조회 (블랙홀 클릭 시)
     */
    public void view() {
        this.viewCount++;
        this.isAcknowledged = true;
    }

    /**
     * 공개 설정 토글
     */
    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }

    /**
     * 콘텐츠 재생성 (AI 재호출)
     */
    public void regenerate(String newContent) {
        this.content = newContent;
    }

    /**
     * 제목 자동 생성
     */
    public static String generateTitle(UUID userId, int sequence) {
        return String.format("나약한 인간의 일기 #%d", sequence);
    }
}