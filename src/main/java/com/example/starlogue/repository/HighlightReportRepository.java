package com.example.starlogue.repository;


import com.example.starlogue.domain.HighlightReport;
import com.example.starlogue.domain.enums.ReportTone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HighlightReportRepository extends JpaRepository<HighlightReport, UUID> {

    // DailyRecord ID로 조회
    Optional<HighlightReport> findByDailyRecordId(UUID dailyRecordId);

    // 특정 날짜의 리포트
    @Query("SELECT hr FROM HighlightReport hr " +
            "JOIN hr.dailyRecord dr " +
            "WHERE dr.user.id = :userId AND dr.recordDate = :date")
    Optional<HighlightReport> findByUserIdAndDate(@Param("userId") UUID userId,
                                                  @Param("date") LocalDate date);

    // 최근 리포트 목록
    @Query("SELECT hr FROM HighlightReport hr " +
            "JOIN hr.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY dr.recordDate DESC LIMIT :limit")
    List<HighlightReport> findRecentReports(@Param("userId") UUID userId,
                                            @Param("limit") int limit);

    // 톤별 리포트 조회
    @Query("SELECT hr FROM HighlightReport hr " +
            "JOIN hr.dailyRecord dr " +
            "WHERE dr.user.id = :userId AND hr.tone = :tone " +
            "ORDER BY dr.recordDate DESC")
    List<HighlightReport> findByUserIdAndTone(@Param("userId") UUID userId,
                                              @Param("tone") ReportTone tone);

    // MVP 시간이 긴 리포트 (베스트 집중 기록)
    @Query("SELECT hr FROM HighlightReport hr " +
            "JOIN hr.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY hr.mvpDurationMinutes DESC LIMIT :limit")
    List<HighlightReport> findTopByMvpDuration(@Param("userId") UUID userId,
                                               @Param("limit") int limit);

    // 집중률이 높은 리포트
    @Query("SELECT hr FROM HighlightReport hr " +
            "JOIN hr.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY hr.focusRate DESC LIMIT :limit")
    List<HighlightReport> findTopByFocusRate(@Param("userId") UUID userId,
                                             @Param("limit") int limit);
}
