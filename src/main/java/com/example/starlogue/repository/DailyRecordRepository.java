package com.example.starlogue.repository;

import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, UUID> {

    // === 단일 조회 ===

    // 특정 날짜의 기록
    Optional<DailyRecord> findByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    // 오늘 기록 존재 여부
    boolean existsByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    // === 기간 조회 (공부 은하수 시각화) ===

    // 기간 내 기록들 (날짜 내림차순)
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "AND dr.recordDate BETWEEN :startDate AND :endDate " +
            "ORDER BY dr.recordDate DESC")
    List<DailyRecord> findByUserIdAndPeriod(@Param("userId") UUID userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // 최근 N일 기록
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "ORDER BY dr.recordDate DESC LIMIT :days")
    List<DailyRecord> findRecentRecords(@Param("userId") UUID userId,
                                        @Param("days") int days);

    // 페이징 조회
    Page<DailyRecord> findByUserIdOrderByRecordDateDesc(UUID userId, Pageable pageable);

    // === 타입별 조회 ===

    // 별(성공) 기록만
    List<DailyRecord> findByUserIdAndRecordTypeOrderByRecordDateDesc(UUID userId,
                                                                     RecordType recordType);

    // 블랙홀(실패) 기록만
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "AND dr.recordType IN ('BLACK_HOLE', 'METEORITE') " +
            "ORDER BY dr.recordDate DESC")
    List<DailyRecord> findFailedRecords(@Param("userId") UUID userId);

    // === Streak 관련 ===

    // 연속 성공 시작점들 (별자리 연결선 시작)
    List<DailyRecord> findByUserIdAndIsStreakStartTrueOrderByRecordDateDesc(UUID userId);

    // 특정 streak에 포함된 기록들 (streakDay > 0인 연속 기록)
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "AND dr.recordType = 'STAR' AND dr.streakDay > 0 " +
            "AND dr.recordDate <= :endDate " +
            "ORDER BY dr.recordDate DESC")
    List<DailyRecord> findStreakRecords(@Param("userId") UUID userId,
                                        @Param("endDate") LocalDate endDate);

    // === 통계용 쿼리 ===

    // 총 별 개수
    int countByUserIdAndRecordType(UUID userId, RecordType recordType);

    // 기간 내 별 개수
    @Query("SELECT COUNT(dr) FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "AND dr.recordType = 'STAR' " +
            "AND dr.recordDate BETWEEN :startDate AND :endDate")
    int countStarsByPeriod(@Param("userId") UUID userId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);

    // 기간 내 총 공부 시간
    @Query("SELECT COALESCE(SUM(dr.totalStudyMinutes), 0) FROM DailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "AND dr.recordDate BETWEEN :startDate AND :endDate")
    int sumStudyMinutesByPeriod(@Param("userId") UUID userId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    // 월별 통계 (년-월, 별 수, 총 공부시간)
    @Query("SELECT YEAR(dr.recordDate), MONTH(dr.recordDate), " +
            "COUNT(CASE WHEN dr.recordType = 'STAR' THEN 1 END), " +
            "COALESCE(SUM(dr.totalStudyMinutes), 0) " +
            "FROM DailyRecord dr WHERE dr.user.id = :userId " +
            "GROUP BY YEAR(dr.recordDate), MONTH(dr.recordDate) " +
            "ORDER BY YEAR(dr.recordDate) DESC, MONTH(dr.recordDate) DESC")
    List<Object[]> getMonthlyStatistics(@Param("userId") UUID userId);

    // === 초신성(Supernova) 조회 ===

    // 초신성 효과가 있는 기록들 (MVP 구간이 특별히 빛난 날)
    List<DailyRecord> findByUserIdAndHasSupernovaTrueOrderByRecordDateDesc(UUID userId);

    // === 정산 대기 기록 ===

    // 아직 최종 판정되지 않은 기록 (PENDING 상태)
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.recordType = 'PENDING' " +
            "AND dr.recordDate < :today")
    List<DailyRecord> findPendingRecords(@Param("today") LocalDate today);
}