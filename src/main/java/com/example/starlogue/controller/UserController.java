package com.example.starlogue.controller;

import com.example.starlogue.config.CustomUserDetails;
import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.domain.User;
import com.example.starlogue.dto.UserDto;
import com.example.starlogue.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.example.starlogue.dto.UserDto.*;

/**
 * 사용자 API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        User user = userService.getUser(userId);
        return ApiResponse.ok(UserResponse.from(user));
    }

    /**
     * 프로필 업데이트
     * PATCH /api/users/me
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = userDetails.getUserId();
        User user = userService.updateProfile(
                userId,
                request.nickname(),
                request.profileImageUrl()
        );
        return ApiResponse.ok(UserResponse.from(user), "프로필이 업데이트되었습니다.");
    }

    /**
     * 내 통계 조회
     * GET /api/users/me/stats
     */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        User user = userService.getUser(userId);
        return ApiResponse.ok(UserStatsResponse.from(user));
    }

    /**
     * Streak 랭킹 조회
     * GET /api/users/ranking/streak
     */
    @GetMapping("/ranking/streak")
    public ApiResponse<List<UserSimpleResponse>> getStreakRanking(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UserSimpleResponse> ranking = userService.getStreakRanking(limit)
                .stream()
                .map(UserSimpleResponse::from)
                .toList();
        return ApiResponse.ok(ranking);
    }

    /**
     * 공부시간 랭킹 조회
     * GET /api/users/ranking/study-time
     */
    @GetMapping("/ranking/study-time")
    public ApiResponse<List<UserSimpleResponse>> getStudyTimeRanking(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UserSimpleResponse> ranking = userService.getStudyTimeRanking(limit)
                .stream()
                .map(UserSimpleResponse::from)
                .toList();
        return ApiResponse.ok(ranking);
    }
}