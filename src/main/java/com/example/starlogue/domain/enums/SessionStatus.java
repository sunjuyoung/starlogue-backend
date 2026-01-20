package com.example.starlogue.domain.enums;


public enum SessionStatus {
    IN_PROGRESS("진행 중"),
    PAUSED("일시 중단"),
    COMPLETED("성공 완료"),
    FAILED("실패"),
    ABANDONED("포기");  // 사용자가 명시적으로 포기

    private final String description;

    SessionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == ABANDONED;
    }
}
