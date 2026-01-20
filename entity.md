# Repository & Service 계층 작성

---

###  전체 패키지 구조

```
starlogue/
├── domain/                          # 엔티티 + Repository
│   ├── common/
│   │   └── BaseTimeEntity.java
│   ├── user/
│   │   ├── User.java
│   │   ├── AuthProvider.java
│   │   └── UserRepository.java      ← NEW
│   ├── tag/
│   │   ├── Tag.java
│   │   └── TagRepository.java       ← NEW
│   ├── session/
│   │   ├── StudySession.java
│   │   ├── Pledge.java
│   │   ├── StopEvent.java
│   │   ├── StopReason.java
│   │   ├── SessionStatus.java
│   │   ├── StudySessionRepository.java  ← NEW
│   │   └── StopEventRepository.java     ← NEW
│   ├── daily/
│   │   ├── DailyRecord.java
│   │   ├── RecordType.java
│   │   ├── HighlightReport.java
│   │   ├── ReportTone.java
│   │   ├── DailyRecordRepository.java     ← NEW
│   │   └── HighlightReportRepository.java ← NEW
│   └── penalty/
│       ├── DarkHistory.java
│       ├── SatireLevel.java
│       └── DarkHistoryRepository.java  ← NEW
│
└── application/                     # Service 계층
    ├── StudyFacadeService.java      ← NEW (파사드)
    ├── user/
    │   └── UserService.java         ← NEW
    ├── tag/
    │   └── TagService.java          ← NEW
    ├── session/
    │   └── StudySessionService.java ← NEW (핵심!)
    ├── daily/
    │   └── DailyRecordService.java  ← NEW
    ├── penalty/
    │   └── DarkHistoryService.java  ← NEW (AI 연동 준비)
    └── scheduler/
        └── StudyScheduler.java      ← NEW (배치)
```

---

###  핵심 설계 포인트

| 계층 | 설계 이유 |
|------|----------|
| **Repository** | Spring Data JPA + 커스텀 @Query로 복잡한 조회 최적화 |
| **Service** | 각 도메인별 비즈니스 로직 캡슐화 |
| **Facade** | Controller가 여러 Service를 조합하지 않도록 단일 진입점 제공 |
| **Scheduler** | 장기 미활동 세션/미정산 기록 자동 처리 |

###  파사드 패턴 사용 

```java
//  Controller에서 직접 여러 Service 호출 (복잡)
sessionService.endSession(sessionId);
dailyRecordService.addSessionResult(sessionId);
if (failed) darkHistoryService.create(...);

//  Facade를 통해 단일 호출 (간단)
facade.endStudy(sessionId);  // 내부에서 모든 처리
```

