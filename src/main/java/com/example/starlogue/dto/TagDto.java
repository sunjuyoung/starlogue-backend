package com.example.starlogue.dto;

import com.example.starlogue.domain.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Tag 관련 DTO
 */
public class TagDto {

    /**
     * 태그 생성 요청
     */
    public record CreateTagRequest(
            @NotBlank(message = "태그명은 필수입니다")
            @Size(min = 1, max = 20, message = "태그명은 1~20자 사이여야 합니다")
            String name,

            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 HEX 형식이어야 합니다 (예: #FF6B6B)")
            String colorHex,

            @Size(max = 10, message = "아이콘은 10자 이내여야 합니다")
            String icon
    ) {}

    /**
     * 태그 업데이트 요청
     */
    public record UpdateTagRequest(
            @NotBlank(message = "태그명은 필수입니다")
            @Size(min = 1, max = 20, message = "태그명은 1~20자 사이여야 합니다")
            String name,

            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 HEX 형식이어야 합니다")
            String colorHex,

            @Size(max = 10, message = "아이콘은 10자 이내여야 합니다")
            String icon
    ) {}

    // === Response ===

    /**
     * 태그 응답
     */
    public record TagResponse(
            UUID id,
            String name,
            String colorHex,
            String icon,
            int usageCount,
            boolean isActive
    ) {
        public static TagResponse from(Tag tag) {
            return new TagResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getColorHex(),
                    tag.getIcon(),
                    tag.getUsageCount(),
                    tag.getIsActive()
            );
        }
    }
}