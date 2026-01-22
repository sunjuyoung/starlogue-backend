
## ⚙️ Service 계층

```
application/
├── StudyFacadeService.java      # 파사드 (Controller 진입점)
├── user/
│   └── UserService.java
├── tag/
│   └── TagService.java
├── session/
│   └── StudySessionService.java  # 핵심 비즈니스 로직
├── daily/
│   └── DailyRecordService.java
├── penalty/
│   └── DarkHistoryService.java   # AI 연동 준비
└── scheduler/
    └── StudyScheduler.java       # 배치 작업
```

### StudyFacadeService (파사드 패턴)

Controller에서 복잡한 서비스 조합 대신 **단일 진입점** 제공:

```java
// 공부 시작
StudySession session = facade.startStudy(userId, tagId, "2시간 React", 120);

// 공부 종료 → 세션 종료 + DailyRecord 반영 자동 처리
SessionEndResult result = facade.endStudy(sessionId);

// 하루 종료 정산 → 별/블랙홀 판정 + 리포트 + 흑역사 자동 생성
DailyEndResult daily = facade.finalizeDailyStudy(userId, date);
```

### StudyScheduler (배치 작업)

| 작업 | 스케줄 | 설명 |
|------|--------|------|
| `closeStaleSession()` | 매 시간 | 24시간+ 방치 세션 강제 종료 |
| `finalizePendingRecords()` | 새벽 4시 | 미정산 PENDING 기록 일괄 처리 |

---

## 🔄 핵심 플로우

### 세션 라이프사이클

```
startStudy() → [공부 중] → pauseStudy() → [중단] → resumeStudy() → [공부 중] → endStudy()
                              │                                          │
                              └──────────────── 반복 가능 ──────────────┘
```

### 하루 종료 정산 플로우

```
finalizeDailyStudy()
    │
    ├─→ 진행 중 세션 강제 종료 (있으면)
    │
    ├─→ 미연결 세션들 DailyRecord에 연결
    │
    ├─→ DailyRecord 최종 판정 (별/블랙홀/운석)
    │
    ├─→ User 통계 갱신 (streak, totalStars 등)
    │
    ├─→ HighlightReport 생성
    │
    └─→ [블랙홀이면] DarkHistory 생성 (AI)
```
