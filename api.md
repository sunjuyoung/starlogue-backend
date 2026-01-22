

## 🌐 API 계층

### 패키지 구조

```
api/
├── common/
│   ├── ApiResponse.java              # 공통 응답 래퍼
│   └── GlobalExceptionHandler.java   # 전역 예외 처리
├── user/
│   ├── UserController.java
│   └── dto/UserDto.java
├── tag/
│   ├── TagController.java
│   └── dto/TagDto.java
├── session/
│   ├── StudySessionController.java   # 핵심!
│   └── dto/SessionDto.java
├── daily/
│   ├── DailyRecordController.java    # 공부 은하수
│   └── dto/DailyDto.java
└── penalty/
    ├── DarkHistoryController.java    # 흑역사
    └── dto/DarkHistoryDto.java
```

### API 엔드포인트 목록

#### 🔐 User API (`/api/users`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/me` | 내 정보 조회 |
| PATCH | `/me` | 프로필 업데이트 |
| GET | `/me/stats` | 내 통계 |
| GET | `/ranking/streak` | Streak 랭킹 |
| GET | `/ranking/study-time` | 공부시간 랭킹 |

#### 🏷️ Tag API (`/api/tags`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 내 태그 목록 |
| POST | `/` | 태그 생성 |
| PATCH | `/{tagId}` | 태그 수정 |
| DELETE | `/{tagId}` | 태그 비활성화 |
| GET | `/popular` | 인기 태그 |

#### 📚 Session API (`/api/sessions`) - 핵심
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/start` | 공부 시작 |
| POST | `/{id}/stop` | 중단 (Stop) |
| POST | `/{id}/resume` | 재개 (Resume) |
| POST | `/{id}/end` | 종료 |
| POST | `/{id}/abandon` | 포기 |
| GET | `/current` | 현재 상태 |
| GET | `/{id}/stop-events` | 중단 이벤트 목록 |
| GET | `/date/{date}` | 날짜별 세션 |
| GET | `/history` | 히스토리 (페이징) |

#### ⭐ Daily API (`/api/daily`) - 공부 은하수
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/galaxy` | 은하수 뷰 (기간) |
| GET | `/galaxy/recent` | 최근 N일 |
| GET | `/today` | 오늘 기록 |
| GET | `/{date}` | 날짜별 기록 |
| GET | `/{date}/detail` | 상세 (리포트+흑역사) |
| GET | `/{date}/report` | 하이라이트 리포트 |
| POST | `/{date}/finalize` | 하루 종료 정산 |
| GET | `/stats/monthly` | 월별 통계 |
| GET | `/streaks` | Streak 기록 |

#### 🕳️ DarkHistory API (`/api/dark-histories`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 내 흑역사 목록 |
| GET | `/unacknowledged` | 미확인 흑역사 |
| GET | `/{id}` | 흑역사 상세 (조회수↑) |
| POST | `/{id}/regenerate` | AI 재생성 |
| POST | `/{id}/toggle-public` | 공개 토글 |
| GET | `/public` | 커뮤니티 흑역사 |
| GET | `/stats` | 내 흑역사 통계 |

### 응답 형식

```json
{
  "success": true,
  "message": "공부를 시작합니다! 화이팅 💪",
  "data": { ... },
  "timestamp": "2025-01-20T10:30:00"
}
```

---








## 📝 다음 단계


**WebSocket** (멀티 디바이스 동기화 + 서버 푸시)
**테스트 코드** 작성