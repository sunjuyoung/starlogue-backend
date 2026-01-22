package com.example.starlogue.service;

import com.example.starlogue.domain.enums.ReportTone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 하이라이트 리포트 AI 서비스
 *
 * 기획서 9: 하루 종료 리포트: 경기 하이라이트 편집
 * - 스토리형 출력으로 사용자 경험 강화
 * - MVP 구간, 위기 순간, 전략 제안 포함
 */
@Slf4j
@Service
public class HighlightReportAiService {

    private final ChatClient highlightClient;
    private final ChatClient strategyClient;

    public HighlightReportAiService(
            @Qualifier("highlightReportChatClient") ChatClient highlightClient,
            @Qualifier("strategyChatClient") ChatClient strategyClient) {
        this.highlightClient = highlightClient;
        this.strategyClient = strategyClient;
    }

    /**
     * 하이라이트 요약 생성
     *
     * @param context 리포트 컨텍스트
     * @return 스토리형 요약
     */
    public String generateSummary(ReportContext context) {
        String prompt = buildSummaryPrompt(context);

        try {
            String response = highlightClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("하이라이트 요약 생성 완료 - date: {}, tone: {}",
                    context.date(), context.tone());

            return response.trim();

        } catch (Exception e) {
            log.error("하이라이트 요약 생성 실패", e);
            return generateFallbackSummary(context);
        }
    }

    /**
     * 전략 제안 생성
     *
     * @param context 리포트 컨텍스트
     * @return 내일을 위한 전략 제안 (50자 이내)
     */
    public String generateStrategy(ReportContext context) {
        String prompt = buildStrategyPrompt(context);

        try {
            String response = strategyClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 50자 제한
            String strategy = response.trim();
            if (strategy.length() > 50) {
                strategy = strategy.substring(0, 47) + "...";
            }

            log.info("전략 제안 생성 완료 - strategy: {}", strategy);
            return strategy;

        } catch (Exception e) {
            log.error("전략 제안 생성 실패", e);
            return generateFallbackStrategy(context);
        }
    }

    /**
     * 위기 순간 해설 생성
     *
     * @param crisisEvents 위기 이벤트 목록
     * @return 해설된 위기 순간들
     */
    public String generateCrisisNarrative(List<CrisisEvent> crisisEvents) {
        if (crisisEvents == null || crisisEvents.isEmpty()) {
            return "위기 없이 순항한 하루였다!";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 위기 순간들을 스포츠 해설처럼 짧게 묘사해주세요.\n\n");

        for (int i = 0; i < crisisEvents.size(); i++) {
            CrisisEvent event = crisisEvents.get(i);
            prompt.append(String.format("%d. %s에 '%s' 사유로 %d분 중단 (예상: %d분)\n",
                    i + 1, event.time(), event.reason(), event.actualMinutes(), event.expectedMinutes()));
        }

        prompt.append("\n각 위기를 한 줄씩, 총 3개 이내로 해설해주세요.");

        try {
            return highlightClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.error("위기 해설 생성 실패", e);
            return "몇 번의 위기가 있었지만, 결국 돌아왔다.";
        }
    }

    // === Prompt Builders ===

    private String buildSummaryPrompt(ReportContext context) {
        String toneInstruction = getToneInstruction(context.tone());

        PromptTemplate template = new PromptTemplate("""
                오늘의 공부 기록을 스포츠 경기 하이라이트처럼 요약해주세요.
                
                [기록 데이터]
                - 날짜: {date}
                - 총 공부 시간: {totalStudyMinutes}분
                - 세션 수: {sessionCount}회
                - 성공 세션: {successCount}회
                - 최대 집중 시간: {maxFocusMinutes}분
                - MVP 구간: {mvpTimeRange}
                - 집중률: {focusRate}%
                
                [톤 가이드]
                {toneInstruction}
                
                200자 이내로 생생하게 요약해주세요.
                숫자 나열보다 스토리로 전달하세요.
                """);

        return template.render(Map.of(
                "date", context.date(),
                "totalStudyMinutes", context.totalStudyMinutes(),
                "sessionCount", context.sessionCount(),
                "successCount", context.successCount(),
                "maxFocusMinutes", context.maxFocusMinutes(),
                "mvpTimeRange", context.mvpTimeRange() != null ? context.mvpTimeRange() : "기록 없음",
                "focusRate", Math.round(context.focusRate() * 100),
                "toneInstruction", toneInstruction
        ));
    }

    private String buildStrategyPrompt(ReportContext context) {
        PromptTemplate template = new PromptTemplate("""
                오늘의 공부 패턴을 분석하고 내일을 위한 전략을 제안해주세요.
                
                [오늘 데이터]
                - 총 공부: {totalStudyMinutes}분
                - 최대 집중: {maxFocusMinutes}분
                - 중단 횟수: {pauseCount}회
                - 약속 어김: {brokenCount}회
                
                [제안 규칙]
                - 구체적인 시간/방법 포함
                - 50자 이내로 한 문장
                - 실행 가능한 것만
                
                예시: "내일은 시작 15분을 워밍업 구간으로 설정해봐"
                """);

        return template.render(Map.of(
                "totalStudyMinutes", context.totalStudyMinutes(),
                "maxFocusMinutes", context.maxFocusMinutes(),
                "pauseCount", context.pauseCount(),
                "brokenCount", context.brokenPromiseCount()
        ));
    }

    private String getToneInstruction(ReportTone tone) {
        return switch (tone) {
            case CELEBRATORY -> "축하하는 톤! 대단한 성과를 칭찬하듯이.";
            case ENCOURAGING -> "격려하는 톤! 좋은 시작을 응원하듯이.";
            case OBJECTIVE -> "객관적인 톤. 사실을 담담하게 전달.";
            case SYMPATHETIC -> "공감하는 톤. 힘든 하루를 위로하듯이.";
        };
    }

    // === Fallbacks ===

    private String generateFallbackSummary(ReportContext context) {
        if (context.successCount() > 0) {
            return String.format(
                    "오늘 %d분간의 여정. %d번의 세션 중 %d번 성공! " +
                            "최대 %d분 연속 집중으로 빛났다.",
                    context.totalStudyMinutes(), context.sessionCount(),
                    context.successCount(), context.maxFocusMinutes()
            );
        } else {
            return String.format(
                    "오늘은 %d분간 도전했다. 비록 목표에는 못 미쳤지만, " +
                            "내일은 다른 이야기가 될 거다.",
                    context.totalStudyMinutes()
            );
        }
    }

    private String generateFallbackStrategy(ReportContext context) {
        if (context.maxFocusMinutes() < 15) {
            return "내일은 시작 10분을 '워밍업 타임'으로 정해봐";
        } else if (context.brokenPromiseCount() > 2) {
            return "휴식 시간을 미리 정하고, 타이머로 관리해봐";
        } else {
            return "오늘의 최대 집중 시간을 내일 목표로 설정해봐";
        }
    }

    // === DTOs ===

    public record ReportContext(
            String date,
            int totalStudyMinutes,
            int sessionCount,
            int successCount,
            int maxFocusMinutes,
            String mvpTimeRange,
            double focusRate,
            int pauseCount,
            int brokenPromiseCount,
            ReportTone tone
    ) {}

    public record CrisisEvent(
            String time,        // "14:30"
            String reason,      // "딴짓"
            int expectedMinutes,
            int actualMinutes
    ) {}
}
