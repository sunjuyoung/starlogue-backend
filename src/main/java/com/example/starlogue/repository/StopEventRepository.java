package com.example.starlogue.repository;

import com.example.starlogue.domain.StopEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StopEventRepository extends JpaRepository<StopEvent, UUID> {

    // 세션의 모든 중단 이벤트 (시간순)
    List<StopEvent> findBySessionIdOrderByStoppedAtAsc(UUID sessionId);

    // 세션의 마지막 중단 이벤트
    Optional<StopEvent> findFirstBySessionIdOrderByStoppedAtDesc(UUID sessionId);

    // 아직 재개되지 않은 중단 이벤트 (현재 중단 중)
    Optional<StopEvent> findBySessionIdAndResumedAtIsNull(UUID sessionId);

    // 세션의 약속 어김 이벤트들
    List<StopEvent> findBySessionIdAndIsBrokenPromiseTrue(UUID sessionId);

    // === 통계용 쿼리 ===

    // 세션의 총 중단 횟수
    int countBySessionId(UUID sessionId);

    // 세션의 약속 어김 횟수
    int countBySessionIdAndIsBrokenPromiseTrue(UUID sessionId);

    // 사유별 중단 횟수 (사용자 전체)
    @Query("SELECT se.reason, COUNT(se) FROM StopEvent se " +
            "JOIN se.session s WHERE s.user.id = :userId " +
            "GROUP BY se.reason ORDER BY COUNT(se) DESC")
    List<Object[]> countByReasonForUser(@Param("userId") UUID userId);

    // 특정 기간 내 중단 이벤트 (위기 분석용)
    @Query("SELECT se FROM StopEvent se JOIN se.session s " +
            "WHERE s.user.id = :userId " +
            "AND se.stoppedAt BETWEEN :start AND :end " +
            "ORDER BY se.stoppedAt ASC")
    List<StopEvent> findByUserIdAndPeriod(@Param("userId") UUID userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // 장기 중단 중인 이벤트 (자동 종료 대상)
    @Query("SELECT se FROM StopEvent se WHERE se.resumedAt IS NULL " +
            "AND se.stoppedAt < :threshold")
    List<StopEvent> findStaleStopEvents(@Param("threshold") LocalDateTime threshold);
}