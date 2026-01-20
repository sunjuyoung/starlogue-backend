package com.example.starlogue.domain;



import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다짐 (베팅 계약)
 * 기획서 3-A: 사용자가 적은 다짐/목표를 바탕으로 "계약(룰)"을 만든다
 *
 * @Embeddable 사용 이유:
 * - 다짐은 세션에 종속적인 값 객체
 * - 별도 테이블보다 같은 테이블에 저장하는 것이 조회 성능에 유리
 * - 세션 없이 다짐만 존재할 수 없음
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pledge {

    @Column(name = "pledge_content", length = 200)
    private String content;  // 다짐 내용 (예: "2시간 동안 React 공부하기")

    @Column(name = "pledge_target_minutes")
    private Integer targetMinutes;  // 목표 시간 (분)

    @Column(name = "pledge_max_stop_count")
    private Integer maxStopCount;  // 허용 중단 횟수 (null이면 무제한)

    @Column(name = "pledge_max_stop_duration_minutes")
    private Integer maxStopDurationMinutes;  // 1회 최대 중단 시간 (분)

    @Builder
    public Pledge(String content, Integer targetMinutes,
                  Integer maxStopCount, Integer maxStopDurationMinutes) {
        this.content = content;
        this.targetMinutes = targetMinutes;
        this.maxStopCount = maxStopCount;
        this.maxStopDurationMinutes = maxStopDurationMinutes;
    }

    /**
     * 빈 다짐 생성 (다짐 없이 시작할 때)
     */
    public static Pledge empty() {
        return new Pledge();
    }

    /**
     * 간단한 다짐 생성 (목표 시간만)
     */
    public static Pledge simple(String content, int targetMinutes) {
        return Pledge.builder()
                .content(content)
                .targetMinutes(targetMinutes)
                .build();
    }

    /**
     * 엄격한 다짐 생성 (모든 조건 설정)
     */
    public static Pledge strict(String content, int targetMinutes,
                                int maxStopCount, int maxStopDurationMinutes) {
        return Pledge.builder()
                .content(content)
                .targetMinutes(targetMinutes)
                .maxStopCount(maxStopCount)
                .maxStopDurationMinutes(maxStopDurationMinutes)
                .build();
    }

    public boolean hasTargetMinutes() {
        return targetMinutes != null && targetMinutes > 0;
    }

    public boolean hasStopLimit() {
        return maxStopCount != null;
    }
}