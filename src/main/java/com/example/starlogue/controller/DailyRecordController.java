package com.example.starlogue.controller;

import com.example.starlogue.config.CustomUserDetails;
import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.HighlightReport;
import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.RecordType;
import com.example.starlogue.dto.DailyDto;
import com.example.starlogue.service.DailyRecordService;
import com.example.starlogue.service.StudyFacadeService;
import com.example.starlogue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.starlogue.dto.DailyDto.*;
import static com.example.starlogue.service.StudyFacadeService.*;

/**
 * 일일 기록 API (공부 은하수)
 *
 * 핵심 기능:
 * - 공부 은하수 시각화 데이터
 * - 일일 기록 상세 조회
 * - 하이라이트 리포트
 * - 통계
 */
@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;
    private final StudyFacadeService facadeService;
    private final UserService userService;

    // === 공부 은하수 (Galaxy View) ===

    /**
     * 공부 은하수 조회 (기간별)
     * GET /api/daily/galaxy
     */
    @GetMapping("/galaxy")
    public ApiResponse<GalaxyViewResponse> getGalaxyView(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID userId = userDetails.getUserId();
        List<DailyRecord> records = dailyRecordService.getGalaxyView(userId, startDate, endDate);
        User user = userService.getUser(userId);

        List<GalaxyNodeResponse> nodes = records.stream()
                .map(GalaxyNodeResponse::from)
                .toList();

        int totalStars = (int) records.stream()
                .filter(r -> r.getRecordType() == RecordType.STAR)
                .count();

        int totalBlackHoles = (int) records.stream()
                .filter(r -> r.getRecordType() == RecordType.BLACK_HOLE)
                .count();

        int totalMinutes = records.stream()
                .mapToInt(DailyRecord::getTotalStudyMinutes)
                .sum();

        GalaxyViewResponse response = new GalaxyViewResponse(
                startDate,
                endDate,
                nodes,
                totalStars,
                totalBlackHoles,
                totalMinutes,
                user.getCurrentStreak()
        );

        return ApiResponse.ok(response);
    }

    /**
     * 최근 N일 은하수 조회
     * GET /api/daily/galaxy/recent
     */
    @GetMapping("/galaxy/recent")
    public ApiResponse<List<GalaxyNodeResponse>> getRecentGalaxy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "30") int days
    ) {
        UUID userId = userDetails.getUserId();
        List<GalaxyNodeResponse> nodes = dailyRecordService.getRecentRecords(userId, days)
                .stream()
                .map(GalaxyNodeResponse::from)
                .toList();
        return ApiResponse.ok(nodes);
    }

    // === 일일 기록 조회 ===

    /**
     * 오늘 기록 조회
     * GET /api/daily/today
     */
    @GetMapping("/today")
    public ApiResponse<DailyRecordResponse> getTodayRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        DailyRecord record = dailyRecordService.getOrCreateTodayRecord(userId);
        return ApiResponse.ok(DailyRecordResponse.from(record));
    }

    /**
     * 특정 날짜 기록 조회
     * GET /api/daily/{date}
     */
    @GetMapping("/{date}")
    public ApiResponse<DailyRecordResponse> getRecordByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID userId = userDetails.getUserId();
        DailyRecord record = dailyRecordService.getRecordRequired(userId, date);
        return ApiResponse.ok(DailyRecordResponse.from(record));
    }

    /**
     * 특정 날짜 상세 조회 (리포트 + 흑역사 포함)
     * GET /api/daily/{date}/detail
     */
    @GetMapping("/{date}/detail")
    public ApiResponse<DailyDetailResponse> getRecordDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID userId = userDetails.getUserId();
        DailyRecord record = dailyRecordService.getRecordRequired(userId, date);
        return ApiResponse.ok(DailyDetailResponse.from(record));
    }

    /**
     * 일일 기록 히스토리 (페이징)
     * GET /api/daily/history
     */
    @GetMapping("/history")
    public ApiResponse<Page<DailyRecordResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        UUID userId = userDetails.getUserId();
        Page<DailyRecordResponse> records = dailyRecordService.getRecords(userId, pageable)
                .map(DailyRecordResponse::from);
        return ApiResponse.ok(records);
    }

    // === 하이라이트 리포트 ===

    /**
     * 하이라이트 리포트 조회
     * GET /api/daily/{date}/report
     */
    @GetMapping("/{date}/report")
    public ApiResponse<HighlightReportResponse> getHighlightReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID userId = userDetails.getUserId();
        DailyRecord record = dailyRecordService.getRecordRequired(userId, date);

        HighlightReport report = record.getHighlightReport();
        if (report == null) {
            // 리포트가 없으면 생성
            report = dailyRecordService.createHighlightReport(record.getId());
        }

        return ApiResponse.ok(HighlightReportResponse.from(report));
    }

    // === 하루 종료 정산 ===

    /**
     * 하루 종료 정산 (수동)
     * POST /api/daily/{date}/finalize
     */
    @PostMapping("/{date}/finalize")
    public ApiResponse<DailyDetailResponse> finalizeDailyRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID userId = userDetails.getUserId();
        DailyEndResult result = facadeService.finalizeDailyStudy(userId, date);
        return ApiResponse.ok(
                DailyDetailResponse.from(result.dailyRecord()),
                result.dailyRecord().isStar()
                        ? "오늘의 별이 생성되었습니다! ⭐"
                        : "내일 다시 도전해요 💪"
        );
    }

    // === 통계 ===

    /**
     * 월별 통계
     * GET /api/daily/stats/monthly
     */
    @GetMapping("/stats/monthly")
    public ApiResponse<List<MonthlyStatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<MonthlyStatsResponse> stats = dailyRecordService.getMonthlyStatistics(userId)
                .stream()
                .map(row -> new MonthlyStatsResponse(
                        ((Number) row[0]).intValue(),  // year
                        ((Number) row[1]).intValue(),  // month
                        ((Number) row[2]).intValue(),  // star count
                        ((Number) row[3]).intValue(),  // total minutes
                        ((Number) row[3]).intValue() / 60  // total hours
                ))
                .toList();
        return ApiResponse.ok(stats);
    }

    /**
     * 기간 통계
     * GET /api/daily/stats/period
     */
    @GetMapping("/stats/period")
    public ApiResponse<PeriodStatsResponse> getPeriodStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID userId = userDetails.getUserId();
        int stars = dailyRecordService.countStars(userId, startDate, endDate);
        int minutes = dailyRecordService.sumStudyMinutes(userId, startDate, endDate);

        return ApiResponse.ok(new PeriodStatsResponse(
                startDate,
                endDate,
                stars,
                minutes,
                minutes / 60
        ));
    }

    /**
     * 기간 통계 응답
     */
    public record PeriodStatsResponse(
            LocalDate startDate,
            LocalDate endDate,
            int starCount,
            int totalStudyMinutes,
            int totalStudyHours
    ) {}

    // === Streak 관련 ===

    /**
     * Streak 기록 조회 (별자리 연결용)
     * GET /api/daily/streaks
     */
    @GetMapping("/streaks")
    public ApiResponse<List<GalaxyNodeResponse>> getStreakRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID userId = userDetails.getUserId();
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<GalaxyNodeResponse> streaks = dailyRecordService.getStreakRecords(userId, endDate)
                .stream()
                .map(GalaxyNodeResponse::from)
                .toList();
        return ApiResponse.ok(streaks);
    }
}