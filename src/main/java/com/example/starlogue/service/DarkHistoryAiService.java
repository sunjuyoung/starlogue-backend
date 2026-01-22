package com.example.starlogue.service;

import com.example.starlogue.domain.enums.SatireLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 흑역사 생성 AI 서비스
 *
 * Spring AI 1.0.3 ChatClient를 사용하여 풍자적인 흑역사 콘텐츠를 생성합니다.
 * 기획서 3-B: 목표 달성 실패 시 AI가 '흑역사'(풍자/블랙코미디 톤)를 생성
 */
@Slf4j
@Service
public class DarkHistoryAiService {

    private final ChatClient chatClient;

    public DarkHistoryAiService(@Qualifier("darkHistoryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 흑역사 콘텐츠 생성
     *
     * @param context 흑역사 생성 컨텍스트
     * @return 생성된 흑역사 텍스트 (30~180자)
     */
    public String generateDarkHistory(DarkHistoryContext context) {
        String prompt = buildPrompt(context);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 길이 검증 및 조정
            String validated = validateAndTrim(response);

            log.info("흑역사 생성 완료 - pledge: {}, level: {}, length: {}",
                    context.pledge(), context.satireLevel(), validated.length());

            return validated;

        } catch (Exception e) {
            log.error("흑역사 생성 실패 - pledge: {}", context.pledge(), e);
            return generateFallbackContent(context);
        }
    }

    /**
     * 프롬프트 구성
     */
    private String buildPrompt(DarkHistoryContext context) {
        String toneGuide = context.satireLevel().getPromptGuideline();

        PromptTemplate template = new PromptTemplate("""
                다음 정보를 바탕으로 흑역사를 작성해주세요.
                
                [오늘의 다짐]
                {pledge}
                
                [실제 결과]
                - 공부 시간: {studyMinutes}분
                - 약속 어김 횟수: {brokenPromiseCount}회
                - 딴짓 자백 횟수: {distractionCount}회
                
                [풍자 톤]
                {toneGuide}
                
                [예시]
                {example}
                
                위 정보를 바탕으로 30~180자 이내의 흑역사를 작성하세요.
                일기체로 작성하되, 인신공격 없이 상황만 풍자하세요.
                """);

        return template.render(Map.of(
                "pledge", context.pledge(),
                "studyMinutes", context.studyMinutes(),
                "brokenPromiseCount", context.brokenPromiseCount(),
                "distractionCount", context.distractionCount(),
                "toneGuide", toneGuide,
                "example", context.satireLevel().getExample()
        ));
    }

    /**
     * 응답 검증 및 길이 조정
     */
    private String validateAndTrim(String response) {
        if (response == null || response.isBlank()) {
            return "기록 실패. 그것이 오늘의 전부다.";
        }

        String trimmed = response.trim();

        // 180자 초과 시 자르기
        if (trimmed.length() > 180) {
            trimmed = trimmed.substring(0, 177) + "...";
        }

        // 30자 미만이면 패딩
        if (trimmed.length() < 30) {
            trimmed = trimmed + " 내일은 다르겠지... 아마도.";
        }

        return trimmed;
    }

    /**
     * AI 호출 실패 시 폴백 콘텐츠
     */
    private String generateFallbackContent(DarkHistoryContext context) {
        return switch (context.satireLevel()) {
            case MILD -> String.format(
                    "'%s' - 시작은 창대했다. 결과는... 내일 다시 쓰자.",
                    truncate(context.pledge(), 30)
            );
            case MODERATE -> String.format(
                    "'%s'라는 다짐, %d분 만에 무너졌다. 약속은 %d번 어겼다. 기록은 정직하다.",
                    truncate(context.pledge(), 20), context.studyMinutes(), context.brokenPromiseCount()
            );
            case STRONG -> String.format(
                    "'딴짓' %d회 자백. 그래도 정직함만큼은 인정한다. '%s'는 내일의 숙제로.",
                    context.distractionCount(), truncate(context.pledge(), 20)
            );
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    /**
     * 흑역사 생성 컨텍스트 DTO
     */
    public record DarkHistoryContext(
            String pledge,
            int studyMinutes,
            int brokenPromiseCount,
            int distractionCount,
            SatireLevel satireLevel
    ) {
        public static DarkHistoryContext of(String pledge, int studyMinutes,
                                            int brokenPromiseCount, int distractionCount,
                                            SatireLevel satireLevel) {
            return new DarkHistoryContext(
                    pledge != null ? pledge : "목표 없이 시작한 공부",
                    studyMinutes,
                    brokenPromiseCount,
                    distractionCount,
                    satireLevel
            );
        }
    }
}
