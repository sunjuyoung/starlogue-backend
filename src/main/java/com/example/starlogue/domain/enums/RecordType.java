package com.example.starlogue.domain.enums;


/**
 * 일일 기록 유형
 * 기획서 8-C: 실패 기록(블랙홀/운석)
 */
public enum RecordType {
    PENDING("판정 대기", "하루가 아직 끝나지 않음"),
    STAR("별", "성공한 날 - 밝게 빛나는 별"),
    BLACK_HOLE("블랙홀", "베팅 패배 - 공간을 왜곡하는 블랙홀"),
    METEORITE("운석", "공부 안 함 - 차갑게 식은 운석");

    private final String displayName;
    private final String description;

    RecordType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 시각화에서 사용할 CSS 클래스 또는 이미지 키
     */
    public String getVisualKey() {
        return switch (this) {
            case PENDING -> "pending";
            case STAR -> "star";
            case BLACK_HOLE -> "blackhole";
            case METEORITE -> "meteorite";
        };
    }

    /**
     * 성공 여부
     */
    public boolean isSuccess() {
        return this == STAR;
    }

    /**
     * 흑역사 생성 대상인지
     */
    public boolean requiresDarkHistory() {
        return this == BLACK_HOLE;
    }
}
