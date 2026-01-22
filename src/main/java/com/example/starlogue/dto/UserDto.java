package com.example.starlogue.dto;

import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User 관련 DTO
 */
public class UserDto {

    // === Request ===

    /**
     * 프로필 업데이트 요청
     */
    public record UpdateProfileRequest(
            @NotBlank(message = "닉네임은 필수입니다")
            @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
            String nickname,

            String profileImageUrl
    ) {}

    // === Response ===

    /**
     * 사용자 정보 응답
     */
    public record UserResponse(
            UUID id,
            String email,
            String nickname,
            String profileImageUrl,
            AuthProvider authProvider,
            UserStatsResponse stats,
            LocalDateTime createdAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getAuthProvider(),
                    UserStatsResponse.from(user),
                    user.getCreatedAt()
            );
        }
    }

    /**
     * 사용자 통계 응답
     */
    public record UserStatsResponse(
            int totalStudyMinutes,
            int currentStreak,
            int longestStreak,
            int totalStars,
            int totalBlackHoles
    ) {
        public static UserStatsResponse from(User user) {
            return new UserStatsResponse(
                    user.getTotalStudyMinutes(),
                    user.getCurrentStreak(),
                    user.getLongestStreak(),
                    user.getTotalStars(),
                    user.getTotalBlackHoles()
            );
        }
    }

    /**
     * 간단 사용자 정보 (랭킹 등)
     */
    public record UserSimpleResponse(
            UUID id,
            String nickname,
            String profileImageUrl,
            int currentStreak,
            int totalStudyMinutes
    ) {
        public static UserSimpleResponse from(User user) {
            return new UserSimpleResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getCurrentStreak(),
                    user.getTotalStudyMinutes()
            );
        }
    }
}