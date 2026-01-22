package com.example.starlogue.dto;


import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.enums.SatireLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DarkHistory 관련 DTO
 */
public class DarkHistoryDto {

    // === Response ===

    /**
     * 흑역사 응답
     */
    public record DarkHistoryResponse(
            UUID id,
            UUID dailyRecordId,
            LocalDate recordDate,
            String title,
            String content,
            String originalPledge,
            int failedMinutes,
            int brokenPromiseCount,
            SatireLevel satireLevel,
            int viewCount,
            boolean isPublic,
            boolean isAcknowledged,
            LocalDateTime createdAt
    ) {
        public static DarkHistoryResponse from(DarkHistory dh) {
            return new DarkHistoryResponse(
                    dh.getId(),
                    dh.getDailyRecord().getId(),
                    dh.getDailyRecord().getRecordDate(),
                    dh.getTitle(),
                    dh.getContent(),
                    dh.getOriginalPledge(),
                    dh.getFailedMinutes(),
                    dh.getBrokenPromiseCount(),
                    dh.getSatireLevel(),
                    dh.getViewCount(),
                    dh.getIsPublic(),
                    dh.getIsAcknowledged(),
                    dh.getCreatedAt()
            );
        }
    }

    /**
     * 흑역사 목록 아이템 (간략)
     */
    public record DarkHistoryListItem(
            UUID id,
            LocalDate recordDate,
            String title,
            SatireLevel satireLevel,
            boolean isAcknowledged,
            int viewCount
    ) {
        public static DarkHistoryListItem from(DarkHistory dh) {
            return new DarkHistoryListItem(
                    dh.getId(),
                    dh.getDailyRecord().getRecordDate(),
                    dh.getTitle(),
                    dh.getSatireLevel(),
                    dh.getIsAcknowledged(),
                    dh.getViewCount()
            );
        }
    }

    /**
     * 공개 흑역사 응답 (커뮤니티)
     */
    public record PublicDarkHistoryResponse(
            UUID id,
            String title,
            String content,
            SatireLevel satireLevel,
            int viewCount,
            LocalDate recordDate,
            String authorNickname  // 익명 처리 가능
    ) {
        public static PublicDarkHistoryResponse from(DarkHistory dh, boolean anonymous) {
            return new PublicDarkHistoryResponse(
                    dh.getId(),
                    dh.getTitle(),
                    dh.getContent(),
                    dh.getSatireLevel(),
                    dh.getViewCount(),
                    dh.getDailyRecord().getRecordDate(),
                    anonymous ? "익명의 공부인" : dh.getDailyRecord().getUser().getNickname()
            );
        }
    }

    /**
     * 흑역사 통계 응답
     */
    public record DarkHistoryStatsResponse(
            int totalCount,
            int unacknowledgedCount,
            int publicCount,
            int mildCount,
            int moderateCount,
            int strongCount
    ) {}
}