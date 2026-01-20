package com.example.starlogue.repository;

import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // Streak 랭킹 (상위 N명)
    @Query("SELECT u FROM User u ORDER BY u.currentStreak DESC LIMIT :limit")
    java.util.List<User> findTopByCurrentStreak(@Param("limit") int limit);

    // 총 공부시간 랭킹
    @Query("SELECT u FROM User u ORDER BY u.totalStudyMinutes DESC LIMIT :limit")
    java.util.List<User> findTopByTotalStudyMinutes(@Param("limit") int limit);
}