package com.example.starlogue.config.websocket;

import com.example.starlogue.service.SessionConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SessionConnectionService connectionService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof StompPrincipal principal) {
            UUID userId = principal.userId();
            connectionService.handleConnect(userId, sessionId);
            log.info("WebSocket 연결됨 - userId: {}, wsSessionId: {}", userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof StompPrincipal principal) {
            UUID userId = principal.userId();
            connectionService.handleDisconnect(userId, sessionId);
            log.info("WebSocket 연결 끊김 - userId: {}, wsSessionId: {}", userId, sessionId);
        }
    }
}
