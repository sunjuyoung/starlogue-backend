package com.example.starlogue.controller;

import com.example.starlogue.config.CustomUserDetails;
import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.domain.Tag;
import com.example.starlogue.dto.TagDto;
import com.example.starlogue.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.example.starlogue.dto.TagDto.*;

/**
 * 태그 API
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 내 태그 목록 조회 (활성만)
     * GET /api/tags
     */
    @GetMapping
    public ApiResponse<List<TagResponse>> getMyTags(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<TagResponse> tags = tagService.getActiveTags(userId)
                .stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }

    /**
     * 내 태그 전체 목록 (비활성 포함)
     * GET /api/tags/all
     */
    @GetMapping("/all")
    public ApiResponse<List<TagResponse>> getAllMyTags(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        List<TagResponse> tags = tagService.getAllTags(userId)
                .stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }

    /**
     * 태그 생성
     * POST /api/tags
     */
    @PostMapping
    public ApiResponse<TagResponse> createTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTagRequest request
    ) {
        UUID userId = userDetails.getUserId();
        Tag tag = tagService.createTag(
                userId,
                request.name(),
                request.colorHex(),
                request.icon()
        );
        return ApiResponse.ok(TagResponse.from(tag), "태그가 생성되었습니다.");
    }

    /**
     * 태그 상세 조회
     * GET /api/tags/{tagId}
     */
    @GetMapping("/{tagId}")
    public ApiResponse<TagResponse> getTag(
            @PathVariable UUID tagId
    ) {
        Tag tag = tagService.getTag(tagId);
        return ApiResponse.ok(TagResponse.from(tag));
    }

    /**
     * 태그 업데이트
     * PATCH /api/tags/{tagId}
     */
    @PatchMapping("/{tagId}")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable UUID tagId,
            @Valid @RequestBody UpdateTagRequest request
    ) {
        Tag tag = tagService.updateTag(
                tagId,
                request.name(),
                request.colorHex(),
                request.icon()
        );
        return ApiResponse.ok(TagResponse.from(tag), "태그가 업데이트되었습니다.");
    }

    /**
     * 태그 비활성화 (소프트 삭제)
     * DELETE /api/tags/{tagId}
     */
    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> deactivateTag(
            @PathVariable UUID tagId
    ) {
        tagService.deactivateTag(tagId);
        return ApiResponse.ok("태그가 비활성화되었습니다.");
    }

    /**
     * 태그 재활성화
     * POST /api/tags/{tagId}/activate
     */
    @PostMapping("/{tagId}/activate")
    public ApiResponse<TagResponse> activateTag(
            @PathVariable UUID tagId
    ) {
        tagService.activateTag(tagId);
        Tag tag = tagService.getTag(tagId);
        return ApiResponse.ok(TagResponse.from(tag), "태그가 활성화되었습니다.");
    }

    /**
     * 인기 태그 조회
     * GET /api/tags/popular
     */
    @GetMapping("/popular")
    public ApiResponse<List<TagResponse>> getPopularTags(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit
    ) {
        UUID userId = userDetails.getUserId();
        List<TagResponse> tags = tagService.getTopUsedTags(userId, limit)
                .stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }
}
