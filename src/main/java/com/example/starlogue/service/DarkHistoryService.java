package com.example.starlogue.service;

import com.example.starlogue.domain.DailyRecord;
import com.example.starlogue.domain.DarkHistory;
import com.example.starlogue.domain.StopEvent;
import com.example.starlogue.domain.StudySession;
import com.example.starlogue.domain.enums.SatireLevel;
import com.example.starlogue.repository.DailyRecordRepository;
import com.example.starlogue.repository.DarkHistoryRepository;
import com.example.starlogue.repository.StopEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DarkHistoryService {

    private final DarkHistoryRepository darkHistoryRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final StopEventRepository stopEventRepository;
    // private final AiService aiService; // TODO: Spring AI 연동 시 추가

    /**
     * 흑역사 생성 (실패한 DailyRecord에 대해)
     */
    @Transactional
    public DarkHistory createDarkHistory(UUID dailyRecordId) {
        DailyRecord record = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new IllegalArgumentException("기록을 찾을 수 없습니다."));

        // 이미 흑역사가 있으면 반환
        if (record.getDarkHistory() != null) {
            return record.getDarkHistory();
        }

        // 실패 기록인지 확인
        if (!record.getRecordType().requiresDarkHistory()) {
            throw new IllegalStateException("블랙홀 기록에만 흑역사를 생성할 수 있습니다.");
        }

        // 흑역사 생성에 필요한 데이터 수집
        DarkHistoryContext context = collectContext(record);

        // 풍자 레벨 결정
        SatireLevel satireLevel = determineSatireLevel(context);

        // AI로 흑역사 콘텐츠 생성 (현재는 임시 텍스트)
        String content = generateDarkHistoryContent(context, satireLevel);

        // 제목 생성
        int sequence = darkHistoryRepository.getNextSequenceNumber(record.getUser().getId());
        String title = DarkHistory.generateTitle(record.getUser().getId(), sequence);

        DarkHistory darkHistory = DarkHistory.builder()
                .dailyRecord(record)
                .content(content)
                .title(title)
                .originalPledge(context.pledge())
                .failedMinutes(context.studyMinutes())
                .brokenPromiseCount(context.brokenPromiseCount())
                .satireLevel(satireLevel)
                .build();

        record.attachDarkHistory(darkHistory);
        DarkHistory saved = darkHistoryRepository.save(darkHistory);

        log.info("흑역사 생성 - recordId: {}, satireLevel: {}, title: {}",
                dailyRecordId, satireLevel, title);

        return saved;
    }

    /**
     * 흑역사 조회 (블랙홀 클릭 시)
     */
    @Transactional
    public DarkHistory viewDarkHistory(UUID darkHistoryId) {
        DarkHistory darkHistory = darkHistoryRepository.findById(darkHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("흑역사를 찾을 수 없습니다."));

        darkHistory.view();
        return darkHistory;
    }

    /**
     * DailyRecord ID로 흑역사 조회
     */
    public Optional<DarkHistory> getByDailyRecord(UUID dailyRecordId) {
        return darkHistoryRepository.findByDailyRecordId(dailyRecordId);
    }

    /**
     * 사용자의 흑역사 목록
     */
    public List<DarkHistory> getDarkHistories(UUID userId) {
        return darkHistoryRepository.findByUserId(userId);
    }

    /**
     * 사용자의 흑역사 목록 (페이징)
     */
    public Page<DarkHistory> getDarkHistoriesPaged(UUID userId, Pageable pageable) {
        return darkHistoryRepository.findByUserIdPaged(userId, pageable);
    }

    /**
     * 확인하지 않은 흑역사 목록
     */
    public List<DarkHistory> getUnacknowledgedDarkHistories(UUID userId) {
        return darkHistoryRepository.findUnacknowledged(userId);
    }

    /**
     * 흑역사 공개 설정 토글
     */
    @Transactional
    public DarkHistory togglePublic(UUID darkHistoryId) {
        DarkHistory darkHistory = darkHistoryRepository.findById(darkHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("흑역사를 찾을 수 없습니다."));

        darkHistory.togglePublic();
        log.info("흑역사 공개 설정 변경 - id: {}, isPublic: {}", darkHistoryId, darkHistory.getIsPublic());

        return darkHistory;
    }

    /**
     * 공개된 흑역사 목록 (커뮤니티 피드)
     */
    public Page<DarkHistory> getPublicDarkHistories(Pageable pageable) {
        return darkHistoryRepository.findPublicDarkHistories(pageable);
    }

    /**
     * 흑역사 재생성 (AI 재호출)
     */
    @Transactional
    public DarkHistory regenerateDarkHistory(UUID darkHistoryId) {
        DarkHistory darkHistory = darkHistoryRepository.findById(darkHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("흑역사를 찾을 수 없습니다."));

        DailyRecord record = darkHistory.getDailyRecord();
        DarkHistoryContext context = collectContext(record);

        String newContent = generateDarkHistoryContent(context, darkHistory.getSatireLevel());
        darkHistory.regenerate(newContent);

        log.info("흑역사 재생성 - id: {}", darkHistoryId);

        return darkHistory;
    }

    // === Private Methods ===

    /**
     * 흑역사 생성에 필요한 컨텍스트 수집
     */
    private DarkHistoryContext collectContext(DailyRecord record) {
        List<StudySession> sessions = record.getSessions();

        // 다짐 수집 (첫 번째 세션의 다짐 사용)
        String pledge = sessions.stream()
                .filter(s -> s.getPledge() != null && s.getPledge().getContent() != null)
                .map(s -> s.getPledge().getContent())
                .findFirst()
                .orElse("목표 없이 시작한 공부");

        // 총 공부 시간
        int studyMinutes = record.getTotalStudyMinutes();

        // 약속 어김 횟수 계산
        int brokenPromiseCount = sessions.stream()
                .mapToInt(StudySession::getBrokenPromiseCount)
                .sum();

        // 가장 심각한 약속 어김 정도
        double maxSeverity = sessions.stream()
                .flatMap(s -> stopEventRepository.findBySessionIdAndIsBrokenPromiseTrue(s.getId()).stream())
                .mapToDouble(StopEvent::getBrokenPromiseSeverity)
                .max()
                .orElse(0.0);

        // 딴짓 횟수 (강한 풍자 대상)
        long distractionCount = sessions.stream()
                .flatMap(s -> stopEventRepository.findBySessionIdOrderByStoppedAtAsc(s.getId()).stream())
                .filter(se -> se.getReason().name().equals("DISTRACTION"))
                .count();

        return new DarkHistoryContext(
                pledge, studyMinutes, brokenPromiseCount, maxSeverity, (int) distractionCount
        );
    }

    /**
     * 풍자 레벨 결정
     */
    private SatireLevel determineSatireLevel(DarkHistoryContext context) {
        // 딴짓 자백이 있으면 강한 풍자
        if (context.distractionCount() >= 2) {
            return SatireLevel.STRONG;
        }

        return SatireLevel.determine(context.brokenPromiseCount(), context.maxSeverity());
    }

    /**
     * 흑역사 콘텐츠 생성 (AI 연동 전 임시 구현)
     * TODO: Spring AI 연동 시 실제 AI 호출로 대체
     */
    private String generateDarkHistoryContent(DarkHistoryContext context, SatireLevel level) {
        // 임시 템플릿 기반 생성
        return switch (level) {
            case MILD -> generateMildContent(context);
            case MODERATE -> generateModerateContent(context);
            case STRONG -> generateStrongContent(context);
        };
    }

    private String generateMildContent(DarkHistoryContext context) {
        return String.format(
                "오늘도 용감하게 '%s'를 선언했다. %d분간의 분투 끝에... 내일을 기약하기로 했다. " +
                        "그것만으로도 대단하다. 아마도.",
                context.pledge(), context.studyMinutes()
        );
    }

    private String generateModerateContent(DarkHistoryContext context) {
        return String.format(
                "'%s' - 이 거창한 다짐의 결과는 %d분이었다. " +
                        "약속을 %d번 어긴 것은 덤이다. 수학적으로 계산하면... 그만 알아보자.",
                context.pledge(), context.studyMinutes(), context.brokenPromiseCount()
        );
    }

    private String generateStrongContent(DarkHistoryContext context) {
        return String.format(
                "'%s'라고 적어놓고 '딴짓'을 %d번 자백한 용기만큼은 인정한다. " +
                        "%d분간의 처절한 사투... 의 대부분은 딴짓이었다는 건 안 비밀.",
                context.pledge(), context.distractionCount(), context.studyMinutes()
        );
    }

    /**
     * 흑역사 생성 컨텍스트
     */
    private record DarkHistoryContext(
            String pledge,
            int studyMinutes,
            int brokenPromiseCount,
            double maxSeverity,
            int distractionCount
    ) {}
}
