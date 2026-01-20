package com.example.starlogue.repository;


import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    // === 진행 중인 세션 조회 ===

    // 사용자의 현재 진행 중인 세션 (1개만 허용하는 경우)
    Optional<StudySession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    // 진행 중인 세션 존재 여부
    boolean existsByUserIdAndStatus(UUID userId, SessionStatus status);

    // === 기간별 세션 조회 ===

    // 특정 날짜의 세션들
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId " +
            "AND DATE(s.startedAt) = :date ORDER BY s.startedAt ASC")
    List<StudySession> findByUserIdAndDate(@Param("userId") UUID userId,
                                           @Param("date") LocalDate date);

    // 기간 내 세션들
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId " +
            "AND s.startedAt BETWEEN :start AND :end ORDER BY s.startedAt DESC")
    List<StudySession> findByUserIdAndPeriod(@Param("userId") UUID userId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    // 페이징 처리된 세션 목록
    Page<StudySession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    // === 통계용 쿼리 ===

    // 특정 날짜의 성공 세션 수
    @Query("SELECT COUNT(s) FROM StudySession s WHERE s.user.id = :userId " +
            "AND DATE(s.startedAt) = :date AND s.isSuccess = true")
    int countSuccessSessionsByDate(@Param("userId") UUID userId,
                                   @Param("date") LocalDate date);

    // 특정 날짜의 총 공부 시간 (초)
    @Query("SELECT COALESCE(SUM(s.totalStudySeconds), 0) FROM StudySession s " +
            "WHERE s.user.id = :userId AND DATE(s.startedAt) = :date")
    int sumStudySecondsByDate(@Param("userId") UUID userId,
                              @Param("date") LocalDate date);

    // 특정 날짜의 최대 집중 시간 (초)
    @Query("SELECT COALESCE(MAX(s.maxFocusGauge), 0) FROM StudySession s " +
            "WHERE s.user.id = :userId AND DATE(s.startedAt) = :date")
    int maxFocusGaugeByDate(@Param("userId") UUID userId,
                            @Param("date") LocalDate date);

    // === 태그별 통계 ===

    // 태그별 세션 수
    @Query("SELECT s.tag.id, COUNT(s) FROM StudySession s " +
            "WHERE s.user.id = :userId AND s.tag IS NOT NULL " +
            "GROUP BY s.tag.id ORDER BY COUNT(s) DESC")
    List<Object[]> countSessionsByTag(@Param("userId") UUID userId);

    // 특정 태그의 세션들
    List<StudySession> findByUserIdAndTagIdOrderByStartedAtDesc(UUID userId, UUID tagId);

    // === DailyRecord 연결 ===

    // 아직 DailyRecord에 연결되지 않은 완료된 세션
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId " +
            "AND s.dailyRecord IS NULL " +
            "AND s.status IN ('COMPLETED', 'FAILED', 'ABANDONED') " +
            "AND DATE(s.startedAt) = :date")
    List<StudySession> findUnlinkedSessionsByDate(@Param("userId") UUID userId,
                                                  @Param("date") LocalDate date);

    // === 장기 미활동 세션 처리 ===

    // N시간 이상 진행 중인 세션 (자동 종료 대상)
    @Query("SELECT s FROM StudySession s WHERE s.status = 'IN_PROGRESS' " +
            "AND s.startedAt < :threshold")
    List<StudySession> findStaleInProgressSessions(@Param("threshold") LocalDateTime threshold);
}
