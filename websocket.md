# WebSocket (STOMP) 세션 관리 시스템

## 개요

공부 세션 중 브라우저 닫기, 로그아웃, 네트워크 끊김 등의 상황을 감지하고 **탈주(DESERTED)** 처리하는 실시간 연결 관리 시스템입니다.

---

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser)                         │
├─────────────────────────────────────────────────────────────────┤
│  SockJS + STOMP Client                                          │
│  - 연결: /ws (SockJS endpoint)                                  │
│  - 인증: Authorization 헤더에 JWT 토큰                           │
│  - 구독: /user/queue/* (개인 메시지)                             │
│  - 발행: /app/* (서버로 메시지 전송)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ WebSocket (ws://) / HTTP Fallback
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Server (Spring Boot)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐    ┌──────────────────┐                   │
│  │ WebSocketConfig  │    │ StompAuthChannel │                   │
│  │ (STOMP 브로커)    │    │ Interceptor      │                   │
│  │                  │    │ (JWT 인증)        │                   │
│  └──────────────────┘    └──────────────────┘                   │
│           │                       │                              │
│           ▼                       ▼                              │
│  ┌──────────────────────────────────────────┐                   │
│  │         WebSocketEventListener            │                   │
│  │  - SessionConnectedEvent (연결)           │                   │
│  │  - SessionDisconnectEvent (해제)          │                   │
│  └──────────────────────────────────────────┘                   │
│                          │                                       │
│                          ▼                                       │
│  ┌──────────────────────────────────────────┐                   │
│  │       SessionConnectionService            │                   │
│  │  - 연결 상태 관리 (ConcurrentHashMap)     │                   │
│  │  - 유예 타이머 관리                        │                   │
│  │  - DESERTED 처리                          │                   │
│  └──────────────────────────────────────────┘                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 탈주 감지 흐름

```
[정상 공부 중]
     │
     ▼
[연결 끊김 감지] ─────────────────────────────────┐
     │                                            │
     ▼                                            │
[활성 세션 확인]                                   │
     │                                            │
     ├─ 세션 없음 → 종료                           │
     │                                            │
     ▼                                            │
[유예 타이머 시작] ◄───────────────────────────────┤
     │ (기본 60초)                                 │
     │                                            │
     ├─────────────────────┐                      │
     │                     │                      │
     ▼                     ▼                      │
[유예 시간 내      [유예 시간 초과]                 │
 재연결 성공]           │                         │
     │                  ▼                         │
     ▼             [DESERTED 처리]                │
[타이머 취소]           │                         │
     │                  ▼                         │
     ▼             [DailyRecord 반영]             │
[정상 공부 계속]        │                         │
                       ▼                         │
                  [흑역사 생성 대상]               │
                                                  │
                  [재연결 시] ─────────────────────┘
                       │
                       ▼
                  [새 세션 필요 안내]
```

---

## 파일 구조

```
src/main/java/com/example/starlogue/
├── config/
│   ├── websocket/
│   │   ├── WebSocketConfig.java           # STOMP 브로커 설정
│   │   ├── StompAuthChannelInterceptor.java # JWT 인증 인터셉터
│   │   ├── StompPrincipal.java            # 사용자 Principal
│   │   └── WebSocketEventListener.java    # 연결/해제 이벤트 리스너
│   └── SchedulingConfig.java              # TaskScheduler 빈 설정
├── service/
│   └── SessionConnectionService.java      # 연결 상태 & 유예 타이머 관리
├── controller/
│   └── StudySessionWsController.java      # STOMP 메시지 핸들러
└── domain/
    └── enums/
        └── SessionStatus.java             # DESERTED 상태 추가
```

---

## 주요 컴포넌트

### 1. WebSocketConfig

STOMP 메시지 브로커 설정

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");  // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 발행 prefix
        registry.setUserDestinationPrefix("/user");         // 개인 메시지
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 2. StompAuthChannelInterceptor

STOMP CONNECT 시 JWT 토큰 검증

```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = ...;

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String token = extractToken(accessor);

        if (jwtTokenProvider.validateToken(token)) {
            UUID userId = jwtTokenProvider.getUserId(token);
            accessor.setUser(new StompPrincipal(userId, email));
        }
    }
    return message;
}
```

### 3. SessionConnectionService

연결 상태 관리 및 탈주 처리 핵심 로직

```java
@Service
public class SessionConnectionService {

    // userId -> WebSocket sessionId 매핑
    private final Map<UUID, String> userConnections = new ConcurrentHashMap<>();

    // userId -> 유예 타이머
    private final Map<UUID, ScheduledFuture<?>> gracePeriodTimers = new ConcurrentHashMap<>();

    public void handleDisconnect(UUID userId, String wsSessionId) {
        // 활성 세션 확인
        Optional<StudySession> activeSession = sessionRepository
            .findByUserIdAndStatusIn(userId, IN_PROGRESS, PAUSED);

        if (activeSession.isPresent()) {
            // 유예 타이머 시작
            ScheduledFuture<?> timer = taskScheduler.schedule(
                () -> handleGracePeriodExpired(userId, session.getId()),
                Instant.now().plusSeconds(gracePeriodSeconds)
            );
            gracePeriodTimers.put(userId, timer);
        }
    }

    public void handleGracePeriodExpired(UUID userId, UUID sessionId) {
        if (!userConnections.containsKey(userId)) {
            // 재연결 안됨 → DESERTED 처리
            session.markAsDeserted();
            dailyRecordService.addSessionResult(sessionId);
        }
    }
}
```

---

## STOMP 엔드포인트

### 클라이언트 → 서버

| Destination | 설명 | Payload |
|-------------|------|---------|
| `/app/session/heartbeat` | 집중 시간 업데이트 | `{sessionId, seconds}` |
| `/app/session/status` | 현재 상태 요청 | - |
| `/app/ping` | 연결 유지 확인 | - |

### 서버 → 클라이언트

| Destination | 설명 | Payload |
|-------------|------|---------|
| `/user/queue/session/update` | 세션 상태 업데이트 | `SessionResponse` |
| `/user/queue/session/status` | 현재 상태 응답 | `{hasActiveSession, session}` |
| `/user/queue/session/reconnected` | 재연결 성공 알림 | `{message, timestamp}` |
| `/user/queue/pong` | Ping 응답 | `{type, timestamp}` |

---

## 설정

### application.yaml

```yaml
starlogue:
  session:
    grace-period-seconds: 60  # 유예 시간 (초)
```

### SessionStatus Enum

```java
public enum SessionStatus {
    IN_PROGRESS("진행 중"),
    PAUSED("일시 중단"),
    COMPLETED("성공 완료"),
    FAILED("실패"),
    ABANDONED("포기"),
    DESERTED("탈주");    // 새로 추가

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED
            || this == ABANDONED || this == DESERTED;
    }
}
```

---

## 클라이언트 연동 가이드

### JavaScript (SockJS + STOMP)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  },

  onConnect: () => {
    console.log('WebSocket 연결됨');

    // 세션 상태 업데이트 구독
    client.subscribe('/user/queue/session/update', (message) => {
      const session = JSON.parse(message.body);
      updateUI(session);
    });

    // 재연결 알림 구독
    client.subscribe('/user/queue/session/reconnected', (message) => {
      showToast('재연결되었습니다!');
    });

    // Heartbeat 전송 (30초마다)
    startHeartbeat();
  },

  onDisconnect: () => {
    console.log('WebSocket 연결 끊김');
  },

  onStompError: (frame) => {
    console.error('STOMP 에러:', frame.headers['message']);
  }
});

client.activate();

// Heartbeat 전송
function startHeartbeat() {
  setInterval(() => {
    if (currentSessionId) {
      client.publish({
        destination: '/app/session/heartbeat',
        body: JSON.stringify({
          sessionId: currentSessionId,
          seconds: 30
        })
      });
    }
  }, 30000);
}

// 현재 상태 요청 (재연결 시)
function requestStatus() {
  client.publish({
    destination: '/app/session/status'
  });
}
```

### React Hook 예시

```typescript
function useStudySession() {
  const [client, setClient] = useState<Client | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },

      onConnect: () => {
        setIsConnected(true);

        stompClient.subscribe('/user/queue/session/update', (msg) => {
          setSession(JSON.parse(msg.body));
        });
      },

      onDisconnect: () => setIsConnected(false)
    });

    stompClient.activate();
    setClient(stompClient);

    return () => {
      stompClient.deactivate();
    };
  }, [token]);

  const sendHeartbeat = useCallback((sessionId: string, seconds: number) => {
    client?.publish({
      destination: '/app/session/heartbeat',
      body: JSON.stringify({ sessionId, seconds })
    });
  }, [client]);

  return { session, isConnected, sendHeartbeat };
}
```

---

## 테스트 시나리오

### 1. 정상 연결/해제

```
1. 로그인 후 WebSocket 연결
2. 세션 시작 (/api/sessions/start)
3. Heartbeat 전송 확인
4. 정상 종료 (/api/sessions/{id}/end)
5. WebSocket 연결 해제
```

### 2. 브라우저 닫기 (탈주)

```
1. 세션 진행 중 브라우저 강제 종료
2. 서버에서 연결 끊김 감지
3. 60초 유예 타이머 시작
4. 60초 후 DESERTED 처리
5. DailyRecord에 실패로 기록
```

### 3. 재연결 성공

```
1. 세션 진행 중 네트워크 일시 끊김
2. 서버에서 연결 끊김 감지
3. 유예 타이머 시작
4. 30초 후 재연결
5. 타이머 취소, 세션 계속
6. /user/queue/session/reconnected 메시지 수신
```

### 4. 다중 탭 처리

```
1. 탭 A에서 세션 진행 중
2. 탭 B 열어서 동일 계정 로그인
3. 탭 A 닫기
4. 탭 B가 여전히 연결 → 탈주 처리 안됨
```

---

## 향후 확장 가능 기능

1. **같이 공부방**: `/topic/room/{roomId}` 구독으로 실시간 참여자 상태 공유
2. **실시간 랭킹**: 현재 공부 중인 사용자 목록 브로드캐스트
3. **알림 시스템**: 친구 공부 시작/종료 알림
4. **타이머 동기화**: 서버 기준 타이머로 클라이언트 시간 보정

---

## 의존성

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

---

## 참고

- [Spring WebSocket 공식 문서](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol](https://stomp.github.io/stomp-specification-1.2.html)
- [SockJS](https://github.com/sockjs/sockjs-client)
