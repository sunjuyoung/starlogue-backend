package com.example.starlogue.domain.enums;


/**
 * 중단 사유
 * 기획서 6-C: 사유별 가중치(페널티 차등)
 * - 화장실 / 방해: 페널티 낮음
 * - 휴식: 중간
 * - 딴짓: 높음 (자백이므로 리스크 증가)
 */
public enum StopReason {
    BATHROOM("화장실", 5, "불가피한 생리현상"),
    INTERRUPTION("방해", 5, "외부 요인에 의한 중단"),
    REST("휴식", 10, "의도적인 휴식"),
    DISTRACTION("딴짓", 20, "자백: 집중력 상실");  // 정직한 자백에 높은 페널티

    private final String displayName;
    private final int staminaPenalty;
    private final String description;

    StopReason(String displayName, int staminaPenalty, String description) {
        this.displayName = displayName;
        this.staminaPenalty = staminaPenalty;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStaminaPenalty() {
        return staminaPenalty;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 흑역사 생성 시 사용할 풍자 레벨
     * 딴짓은 자백이므로 더 강한 풍자 대상
     */
    public int getSatireLevel() {
        return switch (this) {
            case BATHROOM, INTERRUPTION -> 1;  // 약한 풍자
            case REST -> 2;                     // 중간 풍자
            case DISTRACTION -> 3;              // 강한 풍자
        };
    }
}
