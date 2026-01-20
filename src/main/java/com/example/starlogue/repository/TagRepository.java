package com.example.starlogue.repository;

import com.example.starlogue.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    // 사용자의 활성 태그 목록 (사용 빈도순)
    List<Tag> findByUserIdAndIsActiveTrueOrderByUsageCountDesc(UUID userId);

    // 사용자의 모든 태그 (비활성 포함)
    List<Tag> findByUserIdOrderByUsageCountDesc(UUID userId);

    // 사용자의 특정 이름 태그 조회
    Optional<Tag> findByUserIdAndName(UUID userId, String name);

    // 태그명 중복 체크
    boolean existsByUserIdAndName(UUID userId, String name);

    // 가장 많이 사용된 태그 (상위 N개)
    @Query("SELECT t FROM Tag t WHERE t.user.id = :userId AND t.isActive = true " +
            "ORDER BY t.usageCount DESC LIMIT :limit")
    List<Tag> findTopUsedTags(@Param("userId") UUID userId, @Param("limit") int limit);
}
