package com.example.starlogue.controller;

import com.example.starlogue.config.websocket.StompPrincipal;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.dto.SessionDto.SessionResponse;
import com.example.starlogue.service.SessionConnectionService;
import com.example.starlogue.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket STOMP 메시지 핸들러
 *
 * 클라이언트 -> 서버: /app/* 경로로 메시지 전송
 * 서버 -> 클라이언트: /user/queue/* 경로로 개인 메시지 수신
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class StudySessionWsController {

    private final StudySessionService sessionService;
    private final SessionConnectionService connectionService;

    /**
     * 집중 시간 업데이트 (heartbeat)
     * 클라이언트: stompClient.send("/app/session/heartbeat", {}, JSON.stringify({sessionId, seconds}))
     * 응답: /user/queue/session/update
     */
    @MessageMapping("/session/heartbeat")
    @SendToUser("/queue/session/update")
    public SessionResponse handleHeartbeat(@Payload HeartbeatMessage message, Principal principal) {
        UUID userId = getUserId(principal);
        log.debug("Heartbeat 수신 - userId: {}, sessionId: {}, seconds: {}",
                userId, message.sessionId(), message.seconds());

        sessionService.updateFocusTime(message.sessionId(), message.seconds());
        StudySession session = sessionService.getSession(message.sessionId());

        return SessionResponse.from(session);
    }

    /**
     * 현재 상태 요청
     * 클라이언트가 재연결 시 현재 세션 상태를 요청
     */
    @MessageMapping("/session/status")
    @SendToUser("/queue/session/status")
    public Map<String, Object> handleStatusRequest(Principal principal) {
        UUID userId = getUserId(principal);

        return sessionService.getCurrentSession(userId)
                .map(session -> Map.<String, Object>of(
                        "hasActiveSession", true,
                        "session", SessionResponse.from(session),
                        "timestamp", LocalDateTime.now()
                ))
                .orElse(Map.of(
                        "hasActiveSession", false,
                        "timestamp", LocalDateTime.now()
                ));
    }

    /**
     * Ping/Pong - 연결 유지 확인
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Map<String, Object> handlePing(Principal principal) {
        UUID userId = getUserId(principal);
        log.trace("Ping 수신 - userId: {}", userId);

        return Map.of(
                "type", "pong",
                "timestamp", LocalDateTime.now()
        );
    }

    private UUID getUserId(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.userId();
        }
        throw new IllegalStateException("Invalid principal type");
    }

    // === Message DTOs ===

    public record HeartbeatMessage(UUID sessionId, int seconds) {}
}
