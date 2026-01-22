package com.example.starlogue.controller;

import com.example.starlogue.config.CustomUserDetails;
import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.enums.SatireLevel;
import com.example.starlogue.dto.DarkHistoryDto;
import com.example.starlogue.service.DarkHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.example.starlogue.dto.DarkHistoryDto.*;

/**
 * 흑역사 API
 *
 * 기획서 3-B: 목표 달성 실패 시 AI가 '흑역사'(풍자/블랙코미디 톤)를 생성
 *
 * 핵심 기능:
 * - 흑역사 조회/확인
 * - 흑역사 재생성 (AI 재호출)
 * - 공개/비공개 설정
 * - 커뮤니티 공개 흑역사 조회
 */
@RestController
@RequestMapping("/api/dark-histories")
@RequiredArgsConstructor
public class DarkHistoryController {

    private final DarkHistoryService darkHistoryService;

    // === 내 흑역사 조회 ===

    /**
     * 내 흑역사 목록
     * GET /api/dark-histories
     */
    @GetMapping
    public ApiResponse<List<DarkHistoryListItem>> getMyDarkHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<DarkHistoryListItem> list = darkHistoryService.getDarkHistories(userId)
                .stream()
                .map(DarkHistoryListItem::from)
                .toList();
        return ApiResponse.ok(list);
    }

    /**
     * 내 흑역사 목록 (페이징)
     * GET /api/dark-histories/paged
     */
    @GetMapping("/paged")
    public ApiResponse<Page<DarkHistoryListItem>> getMyDarkHistoriesPaged(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UUID userId = userDetails.getUserId();
        Page<DarkHistoryListItem> page = darkHistoryService.getDarkHistoriesPaged(userId, pageable)
                .map(DarkHistoryListItem::from);
        return ApiResponse.ok(page);
    }

    /**
     * 미확인 흑역사 목록
     * GET /api/dark-histories/unacknowledged
     */
    @GetMapping("/unacknowledged")
    public ApiResponse<List<DarkHistoryListItem>> getUnacknowledged(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<DarkHistoryListItem> list = darkHistoryService.getUnacknowledgedDarkHistories(userId)
                .stream()
                .map(DarkHistoryListItem::from)
                .toList();
        return ApiResponse.ok(list);
    }

    // === 흑역사 상세 ===

    /**
     * 흑역사 상세 조회 (조회수 증가)
     * GET /api/dark-histories/{darkHistoryId}
     */
    @GetMapping("/{darkHistoryId}")
    public ApiResponse<DarkHistoryResponse> viewDarkHistory(
            @PathVariable UUID darkHistoryId
    ) {
        DarkHistory dh = darkHistoryService.viewDarkHistory(darkHistoryId);
        return ApiResponse.ok(DarkHistoryResponse.from(dh));
    }

    /**
     * 흑역사 재생성 (AI 재호출)
     * POST /api/dark-histories/{darkHistoryId}/regenerate
     */
    @PostMapping("/{darkHistoryId}/regenerate")
    public ApiResponse<DarkHistoryResponse> regenerateDarkHistory(
            @PathVariable UUID darkHistoryId
    ) {
        DarkHistory dh = darkHistoryService.regenerateDarkHistory(darkHistoryId);
        return ApiResponse.ok(DarkHistoryResponse.from(dh), "흑역사가 재생성되었습니다.");
    }

    // === 공개 설정 ===

    /**
     * 공개/비공개 토글
     * POST /api/dark-histories/{darkHistoryId}/toggle-public
     */
    @PostMapping("/{darkHistoryId}/toggle-public")
    public ApiResponse<DarkHistoryResponse> togglePublic(
            @PathVariable UUID darkHistoryId
    ) {
        DarkHistory dh = darkHistoryService.togglePublic(darkHistoryId);
        String message = dh.getIsPublic()
                ? "흑역사가 공개되었습니다. 다른 사용자들이 볼 수 있습니다."
                : "흑역사가 비공개로 전환되었습니다.";
        return ApiResponse.ok(DarkHistoryResponse.from(dh), message);
    }

    // === 커뮤니티 (공개 흑역사) ===

    /**
     * 공개 흑역사 목록 (인기순)
     * GET /api/dark-histories/public
     */
    @GetMapping("/public")
    public ApiResponse<Page<PublicDarkHistoryResponse>> getPublicDarkHistories(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PublicDarkHistoryResponse> page = darkHistoryService.getPublicDarkHistories(pageable)
                .map(dh -> PublicDarkHistoryResponse.from(dh, true));  // 익명 처리
        return ApiResponse.ok(page);
    }

    // === 통계 ===

    /**
     * 내 흑역사 통계
     * GET /api/dark-histories/stats
     */
    @GetMapping("/stats")
    public ApiResponse<DarkHistoryStatsResponse> getMyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<DarkHistory> all = darkHistoryService.getDarkHistories(userId);

        int totalCount = all.size();
        int unacknowledgedCount = (int) all.stream().filter(dh -> !dh.getIsAcknowledged()).count();
        int publicCount = (int) all.stream().filter(DarkHistory::getIsPublic).count();
        int mildCount = (int) all.stream().filter(dh -> dh.getSatireLevel() == SatireLevel.MILD).count();
        int moderateCount = (int) all.stream().filter(dh -> dh.getSatireLevel() == SatireLevel.MODERATE).count();
        int strongCount = (int) all.stream().filter(dh -> dh.getSatireLevel() == SatireLevel.STRONG).count();

        return ApiResponse.ok(new DarkHistoryStatsResponse(
                totalCount,
                unacknowledgedCount,
                publicCount,
                mildCount,
                moderateCount,
                strongCount
        ));
    }
}