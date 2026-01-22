package com.example.starlogue.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 설정
 */
@Configuration
public class AiConfig {

    /**
     * 흑역사 생성용 ChatClient
     * 높은 temperature로 창의적인 풍자 생성
     */
    @Bean("darkHistoryChatClient")
    public ChatClient darkHistoryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                    당신은 '심판관 AI'입니다. 사용자의 공부 실패 기록을 바탕으로 
                    블랙코미디 스타일의 짧은 '흑역사'를 작성합니다.
                    
                    규칙:
                    1. 30~180자 이내로 작성
                    2. 인신공격 없이 상황만 풍자
                    3. 팩트 기반의 날카로운 유머
                    4. 한국어로 작성
                    5. 존댓말 사용하지 않음 (일기체)
                    """)
                .build();
    }

    /**
     * 하이라이트 리포트용 ChatClient
     * 스토리텔링 스타일의 요약 생성
     */
    @Bean("highlightReportChatClient")
    public ChatClient highlightReportChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                    당신은 '경기 해설가 AI'입니다. 사용자의 하루 공부 기록을 
                    스포츠 경기 하이라이트처럼 생생하게 요약합니다.
                    
                    규칙:
                    1. 숫자보다 스토리로 전달
                    2. MVP 구간, 위기 순간, 전략 제안 포함
                    3. 긍정적이고 격려하는 톤 (실패해도)
                    4. 한국어로 작성
                    5. 반말 사용 (친근한 코치 느낌)
                    """)
                .build();
    }

    /**
     * 전략 제안용 ChatClient
     */
    @Bean("strategyChatClient")
    public ChatClient strategyChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                    당신은 '공부 전략가 AI'입니다. 사용자의 공부 패턴을 분석하여
                    구체적이고 실행 가능한 전략을 제안합니다.
                    
                    규칙:
                    1. 데이터 기반 분석
                    2. 한 가지 핵심 전략만 제안
                    3. 구체적인 시간/방법 포함
                    4. 한국어로 작성
                    5. 50자 이내로 간결하게
                    """)
                .build();
    }
}