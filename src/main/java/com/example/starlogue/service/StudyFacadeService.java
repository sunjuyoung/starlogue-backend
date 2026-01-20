package com.example.starlogue.service;

import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.HighlightReport;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.enums.RecordType;
import com.example.starlogue.domain.enums.SessionStatus;
import com.example.starlogue.domain.enums.StopReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * StudyFacadeService
 *
 * 여러 도메인 서비스를 조합하여 상위 수준의 유스케이스를 제공합니다.
 * Controller에서는 이 Facade를 통해 비즈니스 로직에 접근합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyFacadeService {

    private final StudySessionService sessionService;
    private final DailyRecordService dailyRecordService;
    private final DarkHistoryService darkHistoryService;

    // === 세션 라이프사이클 ===

    /**
     * 공부 시작 (세션 + DailyRecord 초기화)
     */
    @Transactional
    public StudySession startStudy(UUID userId, UUID tagId,
                                   String pledgeContent, Integer targetMinutes) {
        // DailyRecord 준비 (없으면 생성)
        dailyRecordService.getOrCreateTodayRecord(userId);

        // 세션 시작
        return sessionService.startSession(userId, tagId, pledgeContent, targetMinutes);
    }

    /**
     * 공부 중단
     */
    @Transactional
    public StudySession pauseStudy(UUID sessionId, StopReason reason, int expectedMinutes) {
        sessionService.stopSession(sessionId, reason, expectedMinutes);
        return sessionService.getSession(sessionId);
    }

    /**
     * 공부 재개
     */
    @Transactional
    public StudySession resumeStudy(UUID sessionId) {
        return sessionService.resumeSession(sessionId);
    }

    /**
     * 공부 종료 (세션 종료 + DailyRecord 반영)
     */
    @Transactional
    public SessionEndResult endStudy(UUID sessionId) {
        // 세션 종료
        StudySession session = sessionService.endSession(sessionId);

        // DailyRecord에 결과 반영
        dailyRecordService.addSessionResult(sessionId);

        return new SessionEndResult(
                session,
                session.getIsSuccess(),
                session.getTotalStudySeconds() / 60,
                session.getMaxFocusGauge() / 60
        );
    }

    /**
     * 공부 포기
     */
    @Transactional
    public SessionEndResult abandonStudy(UUID sessionId) {
        StudySession session = sessionService.abandonSession(sessionId);
        dailyRecordService.addSessionResult(sessionId);

        return new SessionEndResult(
                session,
                false,
                session.getTotalStudySeconds() / 60,
                session.getMaxFocusGauge() / 60
        );
    }

    // === 하루 종료 처리 ===

    /**
     * 하루 종료 정산 (수동 또는 스케줄러)
     *
     * 1. 미정산 세션 연결
     * 2. DailyRecord 최종 판정
     * 3. 하이라이트 리포트 생성
     * 4. 실패 시 흑역사 생성
     */
    @Transactional
    public DailyEndResult finalizeDailyStudy(UUID userId, LocalDate date) {
        // 진행 중인 세션이 있으면 강제 종료
        sessionService.getCurrentSession(userId).ifPresent(session -> {
            log.warn("하루 종료 시 진행 중인 세션 강제 종료 - sessionId: {}", session.getId());
            endStudy(session.getId());
        });

        // 일일 기록 정산
        DailyRecord record = dailyRecordService.finalizeDailyRecord(userId, date);

        // 하이라이트 리포트 생성
        HighlightReport report = dailyRecordService.createHighlightReport(record.getId());

        // 블랙홀이면 흑역사 생성
        DarkHistory darkHistory = null;
        if (record.getRecordType() == RecordType.BLACK_HOLE) {
            darkHistory = darkHistoryService.createDarkHistory(record.getId());
        }

        log.info("하루 종료 정산 완료 - userId: {}, date: {}, type: {}",
                userId, date, record.getRecordType());

        return new DailyEndResult(record, report, darkHistory);
    }

    /**
     * 오늘 정산 (현재 날짜 기준)
     */
    @Transactional
    public DailyEndResult finalizeTodayStudy(UUID userId) {
        return finalizeDailyStudy(userId, LocalDate.now().minusDays(1)); // 어제 기준
    }

    // === 현재 상태 조회 ===

    /**
     * 현재 공부 상태 조회
     */
    @Transactional(readOnly = true)
    public CurrentStudyStatus getCurrentStatus(UUID userId) {
        Optional<StudySession> activeSession = sessionService.getCurrentSession(userId);
        DailyRecord todayRecord = dailyRecordService.getOrCreateTodayRecord(userId);

        return new CurrentStudyStatus(
                activeSession.orElse(null),
                activeSession.map(StudySession::isPaused).orElse(false),
                todayRecord,
                todayRecord.getTotalStudyMinutes(),
                todayRecord.getSessionCount()
        );
    }

    // === Result Records ===

    public record SessionEndResult(
            StudySession session,
            boolean isSuccess,
            int totalStudyMinutes,
            int maxFocusMinutes
    ) {}

    public record DailyEndResult(
            DailyRecord dailyRecord,
            HighlightReport highlightReport,
            DarkHistory darkHistory // null if success
    ) {}

    public record CurrentStudyStatus(
            StudySession activeSession,  // null if no active session
            boolean isPaused,
            DailyRecord todayRecord,
            int todayStudyMinutes,
            int todaySessionCount
    ) {
        public boolean isStudying() {
            return activeSession != null &&
                    activeSession.getStatus() == SessionStatus.IN_PROGRESS;
        }
    }
}