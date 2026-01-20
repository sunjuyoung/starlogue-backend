package com.example.starlogue.domain.enums;


/**
 * 풍자 레벨
 * 흑역사 생성 시 AI가 참고하는 풍자 강도
 * 약속 어김 정도, 실패 패턴에 따라 결정
 */
public enum SatireLevel {
    MILD("약한 풍자",
            "살짝 찔러보는 정도의 유머",
            "오늘도 용감하게 '시작'은 했다. 그것만으로도 대단하다... 아마도."),

    MODERATE("중간 풍자",
            "적당히 따끔한 블랙코미디",
            "2시간 공부 다짐, 실제 집중 시간 23분. 수학적으로 11.5%의 성공률이다."),

    STRONG("강한 풍자",
            "자백(딴짓)에 대한 정직한 풍자",
            "'딴짓'이라고 정직하게 고백한 용기는 인정한다. 문제는 그 딴짓이 47분이었다는 것.");

    private final String displayName;
    private final String description;
    private final String example;

    SatireLevel(String displayName, String description, String example) {
        this.displayName = displayName;
        this.description = description;
        this.example = example;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getExample() {
        return example;
    }

    /**
     * 약속 어김 횟수와 severity로 풍자 레벨 결정
     */
    public static SatireLevel determine(int brokenPromiseCount, double maxSeverity) {
        if (brokenPromiseCount >= 3 || maxSeverity >= 1.0) {
            return STRONG;
        } else if (brokenPromiseCount >= 1 || maxSeverity >= 0.5) {
            return MODERATE;
        }
        return MILD;
    }

    /**
     * AI 프롬프트에서 사용할 톤 가이드
     */
    public String getPromptGuideline() {
        return switch (this) {
            case MILD -> "가볍고 유머러스하게, 살짝 찌르는 정도로";
            case MODERATE -> "블랙코미디 톤으로, 팩트를 기반으로 따끔하게";
            case STRONG -> "정직한 자백에 대한 날카로운 풍자, 하지만 인신공격은 없이";
        };
    }
}