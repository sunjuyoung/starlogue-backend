package com.example.starlogue.repository;


import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.enums.SatireLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DarkHistoryRepository extends JpaRepository<DarkHistory, UUID> {

    // DailyRecord ID로 조회
    Optional<DarkHistory> findByDailyRecordId(UUID dailyRecordId);

    // 사용자의 모든 흑역사 (최신순)
    @Query("SELECT dh FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY dr.recordDate DESC")
    List<DarkHistory> findByUserId(@Param("userId") UUID userId);

    // 페이징 조회
    @Query("SELECT dh FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY dr.recordDate DESC")
    Page<DarkHistory> findByUserIdPaged(@Param("userId") UUID userId, Pageable pageable);

    // 아직 확인하지 않은 흑역사
    @Query("SELECT dh FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId AND dh.isAcknowledged = false " +
            "ORDER BY dr.recordDate DESC")
    List<DarkHistory> findUnacknowledged(@Param("userId") UUID userId);

    // 공개된 흑역사 (다른 사용자들에게 공유)
    @Query("SELECT dh FROM DarkHistory dh " +
            "WHERE dh.isPublic = true " +
            "ORDER BY dh.viewCount DESC")
    Page<DarkHistory> findPublicDarkHistories(Pageable pageable);

    // 풍자 레벨별 조회
    @Query("SELECT dh FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId AND dh.satireLevel = :level " +
            "ORDER BY dr.recordDate DESC")
    List<DarkHistory> findByUserIdAndSatireLevel(@Param("userId") UUID userId,
                                                 @Param("level") SatireLevel level);

    // 가장 많이 본 흑역사 (사용자별)
    @Query("SELECT dh FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId " +
            "ORDER BY dh.viewCount DESC LIMIT :limit")
    List<DarkHistory> findMostViewed(@Param("userId") UUID userId,
                                     @Param("limit") int limit);

    // 총 흑역사 수
    @Query("SELECT COUNT(dh) FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId")
    int countByUserId(@Param("userId") UUID userId);

    // 흑역사 제목 생성을 위한 시퀀스 번호
    @Query("SELECT COUNT(dh) + 1 FROM DarkHistory dh " +
            "JOIN dh.dailyRecord dr " +
            "WHERE dr.user.id = :userId")
    int getNextSequenceNumber(@Param("userId") UUID userId);
}