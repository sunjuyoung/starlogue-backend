package com.example.starlogue.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 공부 기록 벡터 저장소 서비스
 *
 * pgvector를 사용하여 공부 기록을 임베딩하고 유사 패턴을 검색합니다.
 *
 * 활용 사례:
 * 1. 유사한 실패 패턴 검색 → 맞춤형 전략 제안
 * 2. 과거 성공 기록 검색 → 동기부여 메시지
 * 3. 태그별 공부 패턴 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyVectorService {

    private final VectorStore vectorStore;

    /**
     * 일일 기록을 벡터로 저장
     *
     * @param record 일일 기록 정보
     */
    public void saveRecord(StudyRecordEmbedding record) {
        String content = buildEmbeddingContent(record);

        Document document = new Document(
                record.recordId().toString(),
                content,
                Map.of(
                        "userId", record.userId().toString(),
                        "date", record.date().toString(),
                        "recordType", record.recordType(),
                        "totalMinutes", String.valueOf(record.totalMinutes()),
                        "maxFocusMinutes", String.valueOf(record.maxFocusMinutes()),
                        "tags", String.join(",", record.tags())
                )
        );
        vectorStore.add(List.of(document));

        log.info("벡터 저장 완료 - recordId: {}, date: {}", record.recordId(), record.date());
    }

    /**
     * 유사한 공부 패턴 검색
     *
     * @param userId 사용자 ID
     * @param query 검색 쿼리 (예: "짧은 집중, 잦은 중단")
     * @param topK 반환할 결과 수
     * @return 유사한 기록들
     */
    public List<Document> searchSimilarPatterns(UUID userId, String query, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(builder.eq("userId", userId.toString()).build())
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        log.info("유사 패턴 검색 - userId: {}, query: {}, results: {}",
                userId, query, results.size());

        return results;
    }

    /**
     * 성공 기록만 검색 (동기부여용)
     *
     * @param userId 사용자 ID
     * @param topK 반환할 결과 수
     * @return 성공 기록들
     */
    public List<Document> searchSuccessRecords(UUID userId, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query("성공적인 공부, 높은 집중력, 목표 달성")
                .topK(topK)
                .filterExpression(
                        builder.and(
                                builder.eq("userId", userId.toString()),
                                builder.eq("recordType", "STAR")
                        ).build()
                )
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 태그별 유사 패턴 검색
     *
     * @param userId 사용자 ID
     * @param tag 태그명
     * @param topK 반환할 결과 수
     * @return 해당 태그의 유사 기록들
     */
    public List<Document> searchByTag(UUID userId, String tag, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(tag + " 공부 기록")
                .topK(topK)
                .filterExpression(
                        builder.and(
                                builder.eq("userId", userId.toString()),
                                builder.in("tags", tag)
                        ).build()
                )
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 기록 삭제 (사용자 탈퇴 시)
     */
    public void deleteUserRecords(UUID userId) {
        // pgvector는 ID 기반 삭제 지원
        // 실제 구현은 VectorStore 구현체에 따라 다름
        log.info("사용자 벡터 데이터 삭제 요청 - userId: {}", userId);
    }

    // === Private Methods ===

    /**
     * 임베딩용 텍스트 생성
     */
    private String buildEmbeddingContent(StudyRecordEmbedding record) {
        StringBuilder content = new StringBuilder();

        content.append(String.format("날짜: %s, ", record.date()));
        content.append(String.format("결과: %s, ", record.recordType()));
        content.append(String.format("총 공부시간: %d분, ", record.totalMinutes()));
        content.append(String.format("최대 집중시간: %d분, ", record.maxFocusMinutes()));
        content.append(String.format("세션 수: %d회, ", record.sessionCount()));
        content.append(String.format("성공 세션: %d회, ", record.successCount()));

        if (!record.tags().isEmpty()) {
            content.append(String.format("과목: %s, ", String.join(", ", record.tags())));
        }

        if (record.pledge() != null && !record.pledge().isBlank()) {
            content.append(String.format("다짐: %s, ", record.pledge()));
        }

        // 패턴 분석 텍스트 추가
        content.append(analyzePattern(record));

        return content.toString();
    }

    /**
     * 패턴 분석 텍스트 생성 (검색 최적화용)
     */
    private String analyzePattern(StudyRecordEmbedding record) {
        StringBuilder pattern = new StringBuilder();

        // 집중력 패턴
        if (record.maxFocusMinutes() >= 30) {
            pattern.append("높은 집중력, 장시간 집중 성공, ");
        } else if (record.maxFocusMinutes() >= 15) {
            pattern.append("적절한 집중력, ");
        } else {
            pattern.append("짧은 집중, 잦은 중단, ");
        }

        // 성공/실패 패턴
        if (record.recordType().equals("STAR")) {
            pattern.append("목표 달성, 성공적인 하루, ");
        } else if (record.recordType().equals("BLACK_HOLE")) {
            pattern.append("목표 미달성, 개선 필요, ");
        } else {
            pattern.append("공부 없음, 휴식일, ");
        }

        // 약속 이행 패턴
        if (record.brokenPromiseCount() == 0) {
            pattern.append("약속 준수, 자기관리 성공");
        } else if (record.brokenPromiseCount() <= 2) {
            pattern.append("일부 약속 어김, 개선 여지");
        } else {
            pattern.append("잦은 약속 어김, 시간 관리 필요");
        }

        return pattern.toString();
    }
    // === DTO ===

    public record StudyRecordEmbedding(
            UUID recordId,
            UUID userId,
            LocalDate date,
            String recordType,      // STAR, BLACK_HOLE, METEORITE
            int totalMinutes,
            int maxFocusMinutes,
            int sessionCount,
            int successCount,
            int brokenPromiseCount,
            List<String> tags,
            String pledge
    ) {}
}