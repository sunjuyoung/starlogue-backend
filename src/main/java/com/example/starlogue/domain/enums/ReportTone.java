package com.example.starlogue.domain.enums;

/**
 * 하이라이트 리포트 톤
 * AI가 리포트를 생성할 때 참고하는 톤
 */
public enum ReportTone {
    CELEBRATORY("축하", "대단해요! 오늘 정말 빛났어요 ⭐"),
    ENCOURAGING("격려", "좋은 시작이에요! 내일은 더 빛날 거예요"),
    OBJECTIVE("객관적", "오늘의 기록입니다. 내일 다시 도전해보세요"),
    SYMPATHETIC("공감", "힘든 하루였군요. 쉬어가도 괜찮아요");

    private final String displayName;
    private final String sampleMessage;

    ReportTone(String displayName, String sampleMessage) {
        this.displayName = displayName;
        this.sampleMessage = sampleMessage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSampleMessage() {
        return sampleMessage;
    }
}
