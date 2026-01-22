package com.example.starlogue.controller;

import com.example.starlogue.config.CustomUserDetails;
import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.dto.SessionDto;
import com.example.starlogue.service.StudyFacadeService;
import com.example.starlogue.service.StudySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.starlogue.dto.SessionDto.*;
import static com.example.starlogue.service.StudyFacadeService.*;

/**
 * 공부 세션 API
 *
 * 핵심 기능:
 * - 세션 시작/중단/재개/종료
 * - 실시간 상태 조회
 * - 세션 히스토리 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService sessionService;
    private final StudyFacadeService facadeService;

    // === 세션 라이프사이클 ===

    /**
     * 공부 시작
     * POST /api/sessions/start
     */
    @PostMapping("/start")
    public ApiResponse<SessionResponse> startSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody(required = false) StartSessionRequest request
    ) {
        StudySession session;
        UUID userId = userDetails.getUserId();

        log.info("request {}", request);
        if (request == null) {
            // 다짐 없이 간단 시작
            session = facadeService.startStudy(userId, null, null, null);
        } else {
            session = facadeService.startStudy(
                    userId,
                    request.tagId(),
                    request.pledgeContent(),
                    request.targetMinutes()
            );
        }

        return ApiResponse.ok(SessionResponse.from(session), "공부를 시작합니다! 화이팅 💪");
    }

    /**
     * 공부 중단 (Stop 버튼)
     * POST /api/sessions/{sessionId}/stop
     */
    @PostMapping("/{sessionId}/stop")
    public ApiResponse<SessionResponse> stopSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody StopSessionRequest request
    ) {
        StudySession session = facadeService.pauseStudy(
                sessionId,
                request.reason(),
                request.expectedMinutes()
        );

        String message = switch (request.reason()) {
            case BATHROOM -> "화장실 다녀오세요! ⏸️";
            case INTERRUPTION -> "방해 요소를 처리하고 돌아오세요!";
            case REST -> "잠시 휴식! 너무 오래 쉬지는 마세요 😊";
            case DISTRACTION -> "딴짓 자백... 정직함이 미덕이죠 😅";
        };

        return ApiResponse.ok(SessionResponse.from(session), message);
    }

    /**
     * 공부 재개 (Resume)
     * POST /api/sessions/{sessionId}/resume
     */
    @PostMapping("/{sessionId}/resume")
    public ApiResponse<SessionResponse> resumeSession(
            @PathVariable UUID sessionId
    ) {
        StudySession session = facadeService.resumeStudy(sessionId);

        String message = session.getBrokenPromiseCount() > 0
                ? "약속을 어겼지만... 다시 시작이 중요해요! 📚"
                : "다시 집중! 이어서 화이팅 🔥";

        return ApiResponse.ok(SessionResponse.from(session), message);
    }

    /**
     * 공부 종료
     * POST /api/sessions/{sessionId}/end
     */
    @PostMapping("/{sessionId}/end")
    public ApiResponse<SessionEndResponse> endSession(
            @PathVariable UUID sessionId
    ) {
        SessionEndResult result = facadeService.endStudy(sessionId);
        return ApiResponse.ok(SessionEndResponse.from(result.session()));
    }

    /**
     * 공부 포기
     * POST /api/sessions/{sessionId}/abandon
     */
    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<SessionEndResponse> abandonSession(
            @PathVariable UUID sessionId
    ) {
        SessionEndResult result = facadeService.abandonStudy(sessionId);
        return ApiResponse.ok(
                SessionEndResponse.from(result.session()),
                "오늘은 여기까지... 내일 다시 도전해요! 🌙"
        );
    }

    // === 상태 조회 ===

    /**
     * 현재 상태 조회
     * GET /api/sessions/current
     */
    @GetMapping("/current")
    public ApiResponse<CurrentStatusResponse> getCurrentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUserId();
        CurrentStudyStatus status = facadeService.getCurrentStatus(userId);

        CurrentStatusResponse response = new CurrentStatusResponse(
                status.activeSession() != null
                        ? SessionResponse.from(status.activeSession()) : null,
                status.isStudying(),
                status.isPaused(),
                status.todayStudyMinutes(),
                status.todaySessionCount()
        );

        return ApiResponse.ok(response);
    }

    /**
     * 세션 상세 조회
     * GET /api/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> getSession(
            @PathVariable UUID sessionId
    ) {
        StudySession session = sessionService.getSession(sessionId);
        return ApiResponse.ok(SessionResponse.from(session));
    }

    /**
     * 세션의 중단 이벤트 목록
     * GET /api/sessions/{sessionId}/stop-events
     */
    @GetMapping("/{sessionId}/stop-events")
    public ApiResponse<List<StopEventResponse>> getStopEvents(
            @PathVariable UUID sessionId
    ) {
        List<StopEventResponse> events = sessionService.getStopEvents(sessionId)
                .stream()
                .map(StopEventResponse::from)
                .toList();
        return ApiResponse.ok(events);
    }

    // === 히스토리 조회 ===

    /**
     * 특정 날짜의 세션 목록
     * GET /api/sessions/date/{date}
     */
    @GetMapping("/date/{date}")
    public ApiResponse<List<SessionResponse>> getSessionsByDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID userId = userDetails.getUserId();
        List<SessionResponse> sessions = sessionService.getSessionsByDate(userId, date)
                .stream()
                .map(SessionResponse::from)
                .toList();
        return ApiResponse.ok(sessions);
    }

    /**
     * 세션 히스토리 (페이징)
     * GET /api/sessions/history
     */
    @GetMapping("/history")
    public ApiResponse<Page<SessionResponse>> getSessionHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID userId = userDetails.getUserId();
        Page<SessionResponse> sessions = sessionService.getSessions(userId, pageable)
                .map(SessionResponse::from);
        return ApiResponse.ok(sessions);
    }

    // === 실시간 업데이트 ===

    /**
     * 집중 시간 업데이트 (폴링 방식 - WebSocket 대안)
     * POST /api/sessions/{sessionId}/focus
     */
    @PostMapping("/{sessionId}/focus")
    public ApiResponse<SessionResponse> updateFocusTime(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateFocusRequest request
    ) {
        sessionService.updateFocusTime(sessionId, request.additionalSeconds());
        StudySession session = sessionService.getSession(sessionId);
        return ApiResponse.ok(SessionResponse.from(session));
    }
}