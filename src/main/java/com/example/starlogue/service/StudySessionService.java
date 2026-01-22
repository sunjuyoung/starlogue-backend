package com.example.starlogue.service;

import com.example.starlogue.domain.*;
import com.example.starlogue.domain.enums.SessionStatus;
import com.example.starlogue.domain.enums.StopReason;
import com.example.starlogue.repository.StopEventRepository;
import com.example.starlogue.repository.StudySessionRepository;
import com.example.starlogue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final StopEventRepository stopEventRepository;
    private final UserRepository userRepository;
    private final TagService tagService;

    // === 세션 시작 ===

    /**
     * 새 세션 시작
     *
     * @param userId 사용자 ID
     * @param tagId 태그 ID (nullable)
     * @param pledgeContent 다짐 내용 (nullable)
     * @param targetMinutes 목표 시간 (nullable)
     * @return 생성된 세션
     */
    @Transactional
    public StudySession startSession(UUID userId, UUID tagId,
                                     String pledgeContent, Integer targetMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 진행 중인 세션이 있는지 확인
        Optional<StudySession> existingSession = sessionRepository
                .findByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);

        if (existingSession.isPresent()) {
            throw new IllegalStateException("이미 진행 중인 세션이 있습니다. " +
                    "기존 세션을 종료하거나 현재 세션을 계속하세요.");
        }

        // 태그 처리
        Tag tag = null;
        if (tagId != null) {
            tag = tagService.getTag(tagId);
            tagService.incrementUsageCount(tagId);
        }

        // 다짐 생성
        Pledge pledge = createPledge(pledgeContent, targetMinutes);

        // 세션 생성
        StudySession session = StudySession.builder()
                .user(user)
                .tag(tag)
                .pledge(pledge)
                .build();

        StudySession savedSession = sessionRepository.save(session);
        log.info("세션 시작 - userId: {}, sessionId: {}, pledge: {}",
                userId, savedSession.getId(), pledgeContent);

        return savedSession;
    }

    /**
     * 간단 세션 시작 (다짐 없이)
     */
    @Transactional
    public StudySession startSimpleSession(UUID userId, UUID tagId) {
        return startSession(userId, tagId, null, null);
    }

    // === 세션 중단/재개 ===

    /**
     * 세션 중단 (Stop 버튼)
     *
     * @param sessionId 세션 ID
     * @param reason 중단 사유
     * @param expectedMinutes 예상 중단 시간
     * @return 생성된 StopEvent
     */
    @Transactional
    public StopEvent stopSession(UUID sessionId, StopReason reason, int expectedMinutes) {
        StudySession session = getSession(sessionId);

        validateSessionInProgress(session);
        validateNotAlreadyPaused(session);

        StopEvent stopEvent = session.stop(reason, expectedMinutes);
        StopEvent savedEvent = stopEventRepository.save(stopEvent);

        log.info("세션 중단 - sessionId: {}, reason: {}, expected: {}분, stamina: {}",
                sessionId, reason, expectedMinutes, session.getStamina());

        // 스태미나 0이면 세션 실패
        if (session.getStamina() == 0) {
            log.warn("스태미나 소진으로 세션 실패 - sessionId: {}", sessionId);
        }

        return savedEvent;
    }

    /**
     * 세션 재개 (Resume)
     */
    @Transactional
    public StudySession resumeSession(UUID sessionId) {
        StudySession session = getSession(sessionId);

        // 현재 중단 중인 이벤트 찾기
        StopEvent currentStop = stopEventRepository.findBySessionIdAndResumedAtIsNull(sessionId)
                .orElseThrow(() -> new IllegalStateException("현재 중단 중인 상태가 아닙니다."));

        session.resume(currentStop);

        log.info("세션 재개 - sessionId: {}, 약속어김: {}, 실제중단시간: {}초",
                sessionId, currentStop.getIsBrokenPromise(), currentStop.getActualPauseSeconds());

        return session;
    }

    // === 세션 종료 ===

    /**
     * 세션 정상 종료
     */
    @Transactional
    public StudySession endSession(UUID sessionId) {
        StudySession session = getSession(sessionId);

        if (session.getStatus().isFinished()) {
            throw new IllegalStateException("이미 종료된 세션입니다.");
        }

        // 중단 중이면 먼저 재개 처리
        if (session.isPaused()) {
            resumeSession(sessionId);
        }

        session.end();

        log.info("세션 종료 - sessionId: {}, 성공: {}, 총공부시간: {}초, 최대집중: {}초",
                sessionId, session.getIsSuccess(),
                session.getTotalStudySeconds(), session.getMaxFocusGauge());

        return session;
    }

    /**
     * 세션 포기 (명시적 포기)
     */
    @Transactional
    public StudySession abandonSession(UUID sessionId) {
        StudySession session = getSession(sessionId);

        if (session.getStatus().isFinished()) {
            throw new IllegalStateException("이미 종료된 세션입니다.");
        }

        session.forceFailure();
        log.info("세션 포기 - sessionId: {}", sessionId);

        return session;
    }

    // === 집중 시간 업데이트 ===

    /**
     * 집중 시간 업데이트 (프론트에서 주기적 호출 또는 WebSocket)
     */
    @Transactional
    public void updateFocusTime(UUID sessionId, int additionalSeconds) {
        StudySession session = getSession(sessionId);

        if (!session.getStatus().equals(SessionStatus.IN_PROGRESS)) {
            return;
        }

        if (session.isPaused()) {
            return;  // 중단 중이면 집중 시간 업데이트 안 함
        }

        session.updateFocusGauge(additionalSeconds);
    }

    // === 조회 ===

    /**
     * 세션 조회
     */
    public StudySession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
    }

    /**
     * 세션 조회 (User, Tag fetch join - DTO 변환용)
     */
    public StudySession getSessionWithDetails(UUID sessionId) {
        return sessionRepository.findByIdWithUserAndTag(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
    }

    /**
     * 현재 진행 중인 세션 조회
     */
    public Optional<StudySession> getCurrentSession(UUID userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);
    }

    /**
     * 현재 진행 중인 세션 조회 (User, Tag fetch join - DTO 변환용)
     */
    public Optional<StudySession> getCurrentSessionWithDetails(UUID userId) {
        return sessionRepository.findByUserIdAndStatusWithUserAndTag(userId, SessionStatus.IN_PROGRESS);
    }

    /**
     * 진행 중인 세션 존재 여부
     */
    public boolean hasActiveSession(UUID userId) {
        return sessionRepository.existsByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);
    }

    /**
     * 특정 날짜의 세션 목록
     */
    public List<StudySession> getSessionsByDate(UUID userId, LocalDate date) {
        return sessionRepository.findByUserIdAndDate(userId, date);
    }

    /**
     * 세션 목록 (페이징)
     */
    public Page<StudySession> getSessions(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
    }

    /**
     * 세션의 중단 이벤트 목록
     */
    public List<StopEvent> getStopEvents(UUID sessionId) {
        return stopEventRepository.findBySessionIdOrderByStoppedAtAsc(sessionId);
    }

    // === 통계 ===

    /**
     * 특정 날짜의 총 공부 시간 (분)
     */
    public int getTotalStudyMinutesByDate(UUID userId, LocalDate date) {
        return sessionRepository.sumStudySecondsByDate(userId, date) / 60;
    }

    /**
     * 특정 날짜의 성공 세션 수
     */
    public int getSuccessSessionCountByDate(UUID userId, LocalDate date) {
        return sessionRepository.countSuccessSessionsByDate(userId, date);
    }

    /**
     * 특정 날짜의 최대 집중 시간 (분)
     */
    public int getMaxFocusMinutesByDate(UUID userId, LocalDate date) {
        return sessionRepository.maxFocusGaugeByDate(userId, date) / 60;
    }

    // === 배치 처리 ===

    /**
     * 장기 미활동 세션 자동 종료 (스케줄러에서 호출)
     */
    @Transactional
    public int closeStaleSession(int thresholdHours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(thresholdHours);
        List<StudySession> staleSessions = sessionRepository.findStaleInProgressSessions(threshold);

        for (StudySession session : staleSessions) {
            session.forceFailure();
            log.warn("장기 미활동으로 세션 강제 종료 - sessionId: {}", session.getId());
        }

        return staleSessions.size();
    }

    // === Private Methods ===

    private Pledge createPledge(String content, Integer targetMinutes) {
        if (content == null && targetMinutes == null) {
            return Pledge.empty();
        }
        return Pledge.simple(content, targetMinutes != null ? targetMinutes : 0);
    }

    private void validateSessionInProgress(StudySession session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 세션이 아닙니다. 상태: " + session.getStatus());
        }
    }

    private void validateNotAlreadyPaused(StudySession session) {
        if (session.isPaused()) {
            throw new IllegalStateException("이미 중단 중인 세션입니다.");
        }
    }
}