## Spring AI 통합 (1.1.2 + OpenAI + pgvector)

### 버전 정보
- **Spring AI**: 1.1.2 GA
- **Chat Model**: OpenAI gpt-4o-mini
- **Embedding Model**: text-embedding-3-small
- **Vector Store**: PostgreSQL + pgvector (HNSW 인덱스)

### Artifact 변경 (1.0.x → 1.1.x)
```groovy
// 1.0.x (구버전)
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
implementation 'org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter'

// 1.1.x (신버전)
implementation 'org.springframework.ai:spring-ai-starter-model-openai'
implementation 'org.springframework.ai:spring-ai-starter-vector-store-pgvector'
```

### 패키지 구조

```
infrastructure/
└── ai/
    ├── AiConfig.java                  # ChatClient 빈 설정
    ├── VectorStoreConfig.java         # pgvector 설정
    ├── DarkHistoryAiService.java      # 흑역사 생성 AI
    ├── HighlightReportAiService.java  # 리포트 요약 AI
    └── StudyVectorService.java        # RAG용 벡터 저장소
```

### AI 서비스 역할

| 서비스 | 역할 | 모델 |
|--------|------|------|
| `DarkHistoryAiService` | 풍자적 흑역사 생성 | gpt-4o-mini (temp: 0.9) |
| `HighlightReportAiService` | 스토리형 요약 + 전략 제안 | gpt-4o-mini (temp: 0.7) |
| `StudyVectorService` | 유사 패턴 검색 (RAG) | text-embedding-3-small |

### ChatClient 설정

```java
@Bean("darkHistoryChatClient")
public ChatClient darkHistoryChatClient(ChatClient.Builder builder) {
    return builder
            .defaultSystem("당신은 '심판관 AI'입니다...")
            .build();
}
```

### 흑역사 생성 플로우

```
1. DarkHistoryService.createDarkHistory()
   │
   ├─→ collectContext(): 다짐, 공부시간, 약속어김 수집
   │
   ├─→ determineSatireLevel(): 풍자 레벨 결정
   │      - MILD: 약한 풍자
   │      - MODERATE: 블랙코미디
   │      - STRONG: 날카로운 풍자 (딴짓 자백 시)
   │
   └─→ DarkHistoryAiService.generateDarkHistory()
          │
          ├─→ PromptTemplate으로 프롬프트 구성
          │
          └─→ ChatClient.prompt().user(prompt).call().content()
```

### pgvector RAG 활용

```java
// 유사한 실패 패턴 검색 → 맞춤형 전략 제안
List<Document> similar = vectorService.searchSimilarPatterns(
    userId, 
    "짧은 집중, 잦은 중단", 
    5
);

// 과거 성공 기록 검색 → 동기부여
List<Document> successes = vectorService.searchSuccessRecords(userId, 3);
```

### application.yml 설정

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.8
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        index-type: hnsw
        distance-type: cosine_distance
        dimensions: 1536
        initialize-schema: true

starlogue:
  ai:
    dark-history:
      enabled: true
      temperature: 0.9
    highlight-report:
      enabled: true
      temperature: 0.7
```