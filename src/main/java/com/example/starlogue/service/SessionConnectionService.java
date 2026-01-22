package com.example.starlogue.service;

import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.enums.SessionStatus;
import com.example.starlogue.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * WebSocket 연결 상태 관리 및 탈주(DESERTED) 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionConnectionService {

    private final StudySessionRepository sessionRepository;
    private final DailyRecordService dailyRecordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    // 유예 시간 (초) - 기본 60초
    @Value("${starlogue.session.grace-period-seconds:60}")
    private int gracePeriodSeconds;

    // userId -> WebSocket sessionId 매핑
    private final Map<UUID, String> userConnections = new ConcurrentHashMap<>();

    // userId -> 유예 타이머
    private final Map<UUID, ScheduledFuture<?>> gracePeriodTimers = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 시 처리
     */
    public void handleConnect(UUID userId, String wsSessionId) {
        userConnections.put(userId, wsSessionId);

        // 유예 타이머가 있다면 취소 (재연결 성공)
        ScheduledFuture<?> timer = gracePeriodTimers.remove(userId);
        if (timer != null) {
            timer.cancel(false);
            log.info("재연결 성공 - 유예 타이머 취소 - userId: {}", userId);

            // 재연결 성공 알림
            sendToUser(userId, "/queue/session/reconnected", Map.of(
                    "message", "재연결되었습니다!",
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * WebSocket 연결 해제 시 처리
     */
    public void handleDisconnect(UUID userId, String wsSessionId) {
        String currentWsSessionId = userConnections.get(userId);

        // 다른 세션이 이미 연결되어 있으면 무시 (여러 탭 지원)
        if (currentWsSessionId != null && !currentWsSessionId.equals(wsSessionId)) {
            log.debug("다른 WebSocket 세션 활성 중 - 무시 - userId: {}", userId);
            return;
        }

        userConnections.remove(userId);

        // 진행 중인 공부 세션이 있는지 확인
        Optional<StudySession> activeSession = sessionRepository
                .findByUserIdAndStatusIn(userId, SessionStatus.IN_PROGRESS, SessionStatus.PAUSED);

        if (activeSession.isPresent()) {
            StudySession session = activeSession.get();
            log.info("활성 세션 있음 - 유예 타이머 시작 - userId: {}, sessionId: {}, 유예시간: {}초",
                    userId, session.getId(), gracePeriodSeconds);

            // 유예 타이머 시작
            ScheduledFuture<?> timer = taskScheduler.schedule(
                    () -> handleGracePeriodExpired(userId, session.getId()),
                    Instant.now().plusSeconds(gracePeriodSeconds)
            );
            gracePeriodTimers.put(userId, timer);
        }
    }

    /**
     * 유예 시간 만료 처리 - DESERTED 상태로 변경
     */
    @Transactional
    public void handleGracePeriodExpired(UUID userId, UUID sessionId) {
        gracePeriodTimers.remove(userId);

        // 이미 재연결되었는지 확인
        if (userConnections.containsKey(userId)) {
            log.info("유예 시간 만료 전 재연결됨 - 무시 - userId: {}", userId);
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.getStatus().isFinished()) {
                session.markAsDeserted();
                sessionRepository.save(session);

                // DailyRecord에 반영
                dailyRecordService.addSessionResult(sessionId);

                log.warn("탈주 처리됨 - userId: {}, sessionId: {}", userId, sessionId);
            }
        });
    }

    /**
     * 사용자 연결 상태 확인
     */
    public boolean isUserConnected(UUID userId) {
        return userConnections.containsKey(userId);
    }

    /**
     * 특정 사용자에게 메시지 전송
     */
    public void sendToUser(UUID userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, payload);
    }

    /**
     * 세션 상태 업데이트 브로드캐스트
     */
    public void broadcastSessionUpdate(UUID userId, StudySession session) {
        sendToUser(userId, "/queue/session/update", Map.of(
                "sessionId", session.getId(),
                "status", session.getStatus(),
                "stamina", session.getStamina(),
                "focusGauge", session.getFocusGauge(),
                "totalStudySeconds", session.getTotalStudySeconds(),
                "timestamp", LocalDateTime.now()
        ));
    }
}
