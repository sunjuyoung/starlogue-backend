package com.example.starlogue.dto;


import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.HighlightReport;
import com.example.starlogue.domain.enums.RecordType;
import com.example.starlogue.domain.enums.ReportTone;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DailyRecord 관련 DTO
 */
public class DailyDto {

    // === Response ===

    /**
     * 일일 기록 응답
     */
    public record DailyRecordResponse(
            UUID id,
            LocalDate recordDate,
            RecordType recordType,
            StarResponse star,
            int sessionCount,
            int successSessionCount,
            int failedSessionCount,
            int streakDay,
            boolean isStreakStart,
            boolean hasSupernova,
            boolean hasDarkHistory,
            boolean hasHighlightReport
    ) {
        public static DailyRecordResponse from(DailyRecord record) {
            return new DailyRecordResponse(
                    record.getId(),
                    record.getRecordDate(),
                    record.getRecordType(),
                    record.isStar() ? StarResponse.from(record) : null,
                    record.getSessionCount(),
                    record.getSuccessSessionCount(),
                    record.getFailedSessionCount(),
                    record.getStreakDay(),
                    record.getIsStreakStart(),
                    record.getHasSupernova(),
                    record.getDarkHistory() != null,
                    record.getHighlightReport() != null
            );
        }
    }

    /**
     * 별 속성 응답 (시각화용)
     */
    public record StarResponse(
            int totalStudyMinutes,
            String primaryColorHex,
            int brightness,       // 0~100
            int size,             // 1~5
            int maxFocusMinutes,
            String mvpTimeRange,
            boolean hasSupernova  // 30분+ 연속 집중
    ) {
        public static StarResponse from(DailyRecord record) {
            return new StarResponse(
                    record.getTotalStudyMinutes(),
                    record.getPrimaryColorHex(),
                    record.getBrightness(),
                    record.getSize(),
                    record.getMaxFocusMinutes(),
                    record.getMvpTimeRange(),
                    record.getHasSupernova()
            );
        }
    }

    /**
     * 하이라이트 리포트 응답
     */
    public record HighlightReportResponse(
            UUID id,
            String mvpTimeRange,
            int mvpDurationMinutes,
            String mvpDescription,
            List<String> crisisEvents,
            int totalCrisisCount,
            String strategySuggestion,
            String summary,
            int totalStudyMinutes,
            int totalPauseMinutes,
            double focusRate,
            ReportTone tone
    ) {
        public static HighlightReportResponse from(HighlightReport report) {
            List<String> events = report.getCrisisEvents() != null
                    ? List.of(report.getCrisisEvents().split("\\|"))
                    : List.of();

            return new HighlightReportResponse(
                    report.getId(),
                    report.getMvpTimeRange(),
                    report.getMvpDurationMinutes(),
                    report.getMvpDescription(),
                    events,
                    report.getTotalCrisisCount(),
                    report.getStrategySuggestion(),
                    report.getSummary(),
                    report.getTotalStudyMinutes(),
                    report.getTotalPauseMinutes(),
                    report.getFocusRate(),
                    report.getTone()
            );
        }
    }

    /**
     * 공부 은하수 뷰 응답 (캘린더/맵 시각화)
     */
    public record GalaxyViewResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<GalaxyNodeResponse> nodes,
            int totalStars,
            int totalBlackHoles,
            int totalStudyMinutes,
            int currentStreak
    ) {}

    /**
     * 은하수 노드 (개별 날짜)
     */
    public record GalaxyNodeResponse(
            LocalDate date,
            RecordType type,
            StarResponse star,       // null if not STAR
            int streakDay,
            boolean isStreakStart,
            boolean hasSupernova
    ) {
        public static GalaxyNodeResponse from(DailyRecord record) {
            return new GalaxyNodeResponse(
                    record.getRecordDate(),
                    record.getRecordType(),
                    record.isStar() ? StarResponse.from(record) : null,
                    record.getStreakDay(),
                    record.getIsStreakStart(),
                    record.getHasSupernova()
            );
        }
    }

    /**
     * 월별 통계 응답
     */
    public record MonthlyStatsResponse(
            int year,
            int month,
            int starCount,
            int totalStudyMinutes,
            int totalStudyHours
    ) {}

    /**
     * 일일 기록 상세 응답 (리포트 + 흑역사 포함)
     */
    public record DailyDetailResponse(
            DailyRecordResponse record,
            HighlightReportResponse highlightReport,
            DarkHistorySummary darkHistory
    ) {
        public static DailyDetailResponse from(DailyRecord record) {
            return new DailyDetailResponse(
                    DailyRecordResponse.from(record),
                    record.getHighlightReport() != null
                            ? HighlightReportResponse.from(record.getHighlightReport()) : null,
                    record.getDarkHistory() != null
                            ? DarkHistorySummary.from(record.getDarkHistory()) : null
            );
        }
    }

    /**
     * 흑역사 요약 (상세는 별도 API)
     */
    public record DarkHistorySummary(
            UUID id,
            String title,
            boolean isAcknowledged
    ) {
        public static DarkHistorySummary from(DarkHistory dh) {
            return new DarkHistorySummary(
                    dh.getId(),
                    dh.getTitle(),
                    dh.getIsAcknowledged()
            );
        }
    }
}