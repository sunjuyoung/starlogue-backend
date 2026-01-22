package com.example.starlogue.config.websocket;

import com.example.starlogue.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.getUserId(token);
                String email = jwtTokenProvider.getEmail(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, email, Collections.emptyList());

                accessor.setUser(new StompPrincipal(userId, email));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("WebSocket 연결 인증 성공 - userId: {}", userId);
            } else {
                log.warn("WebSocket 연결 인증 실패 - 유효하지 않은 토큰");
                throw new IllegalArgumentException("Invalid or missing JWT token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 쿼리 파라미터로도 토큰 전달 가능 (SockJS 대비)
        String tokenParam = accessor.getFirstNativeHeader("token");
        return tokenParam;
    }
}
