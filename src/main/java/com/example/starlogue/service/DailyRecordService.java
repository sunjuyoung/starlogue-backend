package com.example.starlogue.service;

import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.HighlightReport;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.RecordType;
import com.example.starlogue.repository.DailyRecordRepository;
import com.example.starlogue.repository.HighlightReportRepository;
import com.example.starlogue.repository.StudySessionRepository;
import com.example.starlogue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final HighlightReportRepository highlightReportRepository;
    private final StudySessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // === 일일 기록 생성/조회 ===

    /**
     * 오늘의 DailyRecord 가져오기 (없으면 생성)
     */
    @Transactional
    public DailyRecord getOrCreateTodayRecord(UUID userId) {
        LocalDate today = LocalDate.now();
        return dailyRecordRepository.findByUserIdAndRecordDate(userId, today)
                .orElseGet(() -> createDailyRecord(userId, today));
    }

    /**
     * 특정 날짜의 DailyRecord 생성
     */
    @Transactional
    public DailyRecord createDailyRecord(UUID userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 존재하면 예외
        if (dailyRecordRepository.existsByUserIdAndRecordDate(userId, date)) {
            throw new IllegalStateException("이미 해당 날짜의 기록이 존재합니다: " + date);
        }

        DailyRecord record = DailyRecord.builder()
                .user(user)
                .recordDate(date)
                .build();

        return dailyRecordRepository.save(record);
    }

    /**
     * 특정 날짜의 기록 조회
     */
    public Optional<DailyRecord> getRecord(UUID userId, LocalDate date) {
        return dailyRecordRepository.findByUserIdAndRecordDate(userId, date);
    }

    /**
     * 기록 조회 (필수)
     */
    public DailyRecord getRecordRequired(UUID userId, LocalDate date) {
        return dailyRecordRepository.findByUserIdAndRecordDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 기록이 없습니다: " + date));
    }

    // === 세션 결과 반영 ===

    /**
     * 세션 종료 시 DailyRecord에 결과 반영
     */
    @Transactional
    public void addSessionResult(UUID sessionId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        if (!session.getStatus().isFinished()) {
            throw new IllegalStateException("아직 종료되지 않은 세션입니다.");
        }

        LocalDate sessionDate = session.getStartedAt().toLocalDate();
        DailyRecord record = dailyRecordRepository.findByUserIdAndRecordDate(
                        session.getUser().getId(), sessionDate)
                .orElseGet(() -> createDailyRecord(session.getUser().getId(), sessionDate));

        // 세션 결과 반영
        record.addSessionResult(session);
        session.linkToDailyRecord(record);

        log.info("세션 결과 반영 - recordId: {}, sessionId: {}, 성공: {}",
                record.getId(), sessionId, session.getIsSuccess());
    }

    // === 일일 정산 ===

    /**
     * 하루 종료 정산 (스케줄러 또는 수동 호출)
     */
    @Transactional
    public DailyRecord finalizeDailyRecord(UUID userId, LocalDate date) {
        DailyRecord record = getRecordRequired(userId, date);

        if (record.getRecordType() != RecordType.PENDING) {
            log.warn("이미 정산된 기록입니다 - recordId: {}, type: {}",
                    record.getId(), record.getRecordType());
            return record;
        }

        // 미연결 세션들 처리
        linkUnlinkedSessions(userId, date);

        // 현재 streak 가져오기
        User user = userRepository.findById(userId).orElseThrow();
        int currentStreak = user.getCurrentStreak();

        // 최종 판정
        record.finalize(record.isStar() ? currentStreak + 1 : 0);

        // User 통계 갱신
        if (record.isStar()) {
            userService.recordDailySuccess(userId);
        } else if (record.isFailed()) {
            userService.recordDailyFailure(userId);
        }

        // 공부 시간 갱신
        userService.addStudyMinutes(userId, record.getTotalStudyMinutes());

        log.info("일일 정산 완료 - userId: {}, date: {}, type: {}, streak: {}",
                userId, date, record.getRecordType(), record.getStreakDay());

        return record;
    }

    /**
     * 미연결 세션들을 DailyRecord에 연결
     */
    @Transactional
    public void linkUnlinkedSessions(UUID userId, LocalDate date) {
        List<StudySession> unlinkedSessions = sessionRepository.findUnlinkedSessionsByDate(userId, date);

        for (StudySession session : unlinkedSessions) {
            addSessionResult(session.getId());
        }

        if (!unlinkedSessions.isEmpty()) {
            log.info("미연결 세션 {} 개 연결 완료 - userId: {}, date: {}",
                    unlinkedSessions.size(), userId, date);
        }
    }

    /**
     * PENDING 상태의 과거 기록 일괄 정산 (배치)
     */
    @Transactional
    public int finalizePendingRecords() {
        LocalDate today = LocalDate.now();
        List<DailyRecord> pendingRecords = dailyRecordRepository.findPendingRecords(today);

        int processed = 0;
        for (DailyRecord record : pendingRecords) {
            try {
                finalizeDailyRecord(record.getUser().getId(), record.getRecordDate());
                processed++;
            } catch (Exception e) {
                log.error("기록 정산 실패 - recordId: {}", record.getId(), e);
            }
        }

        log.info("PENDING 기록 일괄 정산 완료 - 처리: {}/{}", processed, pendingRecords.size());
        return processed;
    }

    // === 하이라이트 리포트 ===

    /**
     * 하이라이트 리포트 생성
     */
    @Transactional
    public HighlightReport createHighlightReport(UUID dailyRecordId) {
        DailyRecord record = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new IllegalArgumentException("기록을 찾을 수 없습니다."));

        // 이미 리포트가 있으면 반환
        if (record.getHighlightReport() != null) {
            return record.getHighlightReport();
        }

        HighlightReport report = HighlightReport.builder()
                .dailyRecord(record)
                .build();

        // 통계 설정
        int totalStudy = record.getTotalStudyMinutes();
        int sessionCount = record.getSessionCount();
        // 총 중단 시간은 세션들에서 합산 (별도 계산 필요)
        int totalPause = calculateTotalPauseMinutes(record);

        report.setStatistics(totalStudy, totalPause, sessionCount);

        // MVP 구간 설정
        if (record.getMaxFocusMinutes() > 0) {
            report.setMvpSection(
                    record.getMvpTimeRange(),
                    record.getMaxFocusMinutes(),
                    String.format("%d분 무중단 집중의 영광", record.getMaxFocusMinutes())
            );
        }

        // 톤 결정
        report.determineTone();

        record.attachHighlightReport(report);
        HighlightReport savedReport = highlightReportRepository.save(report);

        log.info("하이라이트 리포트 생성 - recordId: {}, tone: {}", dailyRecordId, report.getTone());

        return savedReport;
    }

    // === 공부 은하수 조회 ===

    /**
     * 기간 내 기록 조회 (은하수 시각화용)
     */
    public List<DailyRecord> getGalaxyView(UUID userId, LocalDate startDate, LocalDate endDate) {
        return dailyRecordRepository.findByUserIdAndPeriod(userId, startDate, endDate);
    }

    /**
     * 최근 N일 기록
     */
    public List<DailyRecord> getRecentRecords(UUID userId, int days) {
        return dailyRecordRepository.findRecentRecords(userId, days);
    }

    /**
     * 페이징 조회
     */
    public Page<DailyRecord> getRecords(UUID userId, Pageable pageable) {
        return dailyRecordRepository.findByUserIdOrderByRecordDateDesc(userId, pageable);
    }

    /**
     * Streak 기록들 (별자리 연결용)
     */
    public List<DailyRecord> getStreakRecords(UUID userId, LocalDate endDate) {
        return dailyRecordRepository.findStreakRecords(userId, endDate);
    }

    // === 통계 ===

    /**
     * 기간 내 별 개수
     */
    public int countStars(UUID userId, LocalDate startDate, LocalDate endDate) {
        return dailyRecordRepository.countStarsByPeriod(userId, startDate, endDate);
    }

    /**
     * 기간 내 총 공부 시간 (분)
     */
    public int sumStudyMinutes(UUID userId, LocalDate startDate, LocalDate endDate) {
        return dailyRecordRepository.sumStudyMinutesByPeriod(userId, startDate, endDate);
    }

    /**
     * 월별 통계
     */
    public List<Object[]> getMonthlyStatistics(UUID userId) {
        return dailyRecordRepository.getMonthlyStatistics(userId);
    }

    // === Private Methods ===

    private int calculateTotalPauseMinutes(DailyRecord record) {
        return record.getSessions().stream()
                .mapToInt(s -> s.getTotalPauseSeconds() / 60)
                .sum();
    }
}