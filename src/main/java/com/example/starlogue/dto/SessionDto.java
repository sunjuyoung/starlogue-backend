package com.example.starlogue.dto;

import com.example.starlogue.domain.Pledge;
import com.example.starlogue.domain.StopEvent;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.enums.SessionStatus;
import com.example.starlogue.domain.enums.StopReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * StudySession 관련 DTO
 */
public class SessionDto {

    // === Request ===

    /**
     * 세션 시작 요청
     */
    public record StartSessionRequest(
            UUID tagId,  // nullable

            @Size(max = 200, message = "다짐은 200자 이내여야 합니다")
            String pledgeContent,

            @Min(value = 0, message = "목표 시간은 0 이상이어야 합니다")
            @Max(value = 480, message = "목표 시간은 8시간(480분) 이내여야 합니다")
            Integer targetMinutes,

            @Min(value = 0, message = "최대 중단 횟수는 0 이상이어야 합니다")
            @Max(value = 10, message = "최대 중단 횟수는 10 이하여야 합니다")
            Integer maxStopCount,

            @Min(value = 0, message = "최대 중단 시간은 0 이상이어야 합니다")
            @Max(value = 60, message = "최대 중단 시간은 60분 이내여야 합니다")
            Integer maxStopDurationMinutes
    ) {
        public StartSessionRequest {
            // null이면 기본값 적용
            if (maxStopCount == null) maxStopCount = 10;
            if (maxStopDurationMinutes == null) maxStopDurationMinutes = 60;
        }
    }

    /**
     * 세션 중단 요청
     */
    public record StopSessionRequest(
            @NotNull(message = "중단 사유는 필수입니다")
            StopReason reason,

            @Min(value = 1, message = "예상 시간은 1분 이상이어야 합니다")
            @Max(value = 60, message = "예상 시간은 60분 이내여야 합니다")
            int expectedMinutes
    ) {}

    /**
     * 집중 시간 업데이트 요청
     */
    public record UpdateFocusRequest(
            @Min(value = 1, message = "추가 시간은 1초 이상이어야 합니다")
            int additionalSeconds
    ) {}

    // === Response ===

    /**
     * 세션 응답
     */
    public record SessionResponse(
            UUID id,
            UUID userId,
            TagDto.TagResponse tag,
            PledgeResponse pledge,
            SessionStatus status,
            int stamina,
            int focusGauge,
            int maxFocusGauge,
            int totalStudySeconds,
            int totalPauseSeconds,
            int brokenPromiseCount,
            boolean isPaused,
            Boolean isSuccess,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        public static SessionResponse from(StudySession session) {
            return new SessionResponse(
                    session.getId(),
                    session.getUser().getId(),
                    session.getTag() != null ? TagDto.TagResponse.from(session.getTag()) : null,
                    PledgeResponse.from(session.getPledge()),
                    session.getStatus(),
                    session.getStamina(),
                    session.getFocusGauge(),
                    session.getMaxFocusGauge(),
                    session.getTotalStudySeconds(),
                    session.getTotalPauseSeconds(),
                    session.getBrokenPromiseCount(),
                    session.isPaused(),
                    session.getIsSuccess(),
                    session.getStartedAt(),
                    session.getEndedAt()
            );
        }
    }

    /**
     * 다짐 응답
     */
    public record PledgeResponse(
            String content,
            Integer targetMinutes,
            Integer maxStopCount,
            Integer maxStopDurationMinutes
    ) {
        public static PledgeResponse from(Pledge pledge) {
            if (pledge == null) return null;
            return new PledgeResponse(
                    pledge.getContent(),
                    pledge.getTargetMinutes(),
                    pledge.getMaxStopCount(),
                    pledge.getMaxStopDurationMinutes()
            );
        }
    }

    /**
     * 중단 이벤트 응답
     */
    public record StopEventResponse(
            UUID id,
            StopReason reason,
            String reasonDisplayName,
            int expectedMinutes,
            Integer actualPauseSeconds,
            boolean isBrokenPromise,
            Double brokenPromiseSeverity,
            LocalDateTime stoppedAt,
            LocalDateTime resumedAt
    ) {
        public static StopEventResponse from(StopEvent event) {
            return new StopEventResponse(
                    event.getId(),
                    event.getReason(),
                    event.getReason().getDisplayName(),
                    event.getExpectedMinutes(),
                    event.getActualPauseSeconds(),
                    event.getIsBrokenPromise(),
                    event.getIsBrokenPromise() ? event.getBrokenPromiseSeverity() : null,
                    event.getStoppedAt(),
                    event.getResumedAt()
            );
        }
    }

    /**
     * 세션 종료 결과 응답
     */
    public record SessionEndResponse(
            SessionResponse session,
            boolean isSuccess,
            int totalStudyMinutes,
            int maxFocusMinutes,
            String message
    ) {
        public static SessionEndResponse from(StudySession session) {
            String message = session.getIsSuccess()
                    ? "수고했어요! 오늘도 별을 향해 한 걸음 나아갔습니다 ⭐"
                    : "괜찮아요. 내일 다시 도전하면 됩니다 💪";

            return new SessionEndResponse(
                    SessionResponse.from(session),
                    session.getIsSuccess(),
                    session.getTotalStudySeconds() / 60,
                    session.getMaxFocusGauge() / 60,
                    message
            );
        }
    }

    /**
     * 현재 상태 응답
     */
    public record CurrentStatusResponse(
            SessionResponse activeSession,  // null if no active session
            boolean isStudying,
            boolean isPaused,
            int todayStudyMinutes,
            int todaySessionCount
    ) {}
}