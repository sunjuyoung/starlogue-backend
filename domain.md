# Starlogue 도메인 설계 (최종)

---

## 전체 도메인 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Core Domain                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐      1:N      ┌──────────────┐      1:N     ┌──────────┐ │
│  │   User   │──────────────▶│   StudyDay   │─────────────▶│ Session  │ │
│  │  (회원)   │               │  (하루=별)    │              │  (세션)   │ │
│  └────┬─────┘               └──────────────┘              └────┬─────┘ │
│       │                                                        │       │
│       │ 1:1                                                    │ 1:1   │
│       ▼                                                        ▼       │
│  ┌──────────┐                                             ┌──────────┐ │
│  │Character │                                             │   Bet    │ │
│  │ (캐릭터)  │                                             │  (베팅)   │ │
│  └──────────┘                                             └────┬─────┘ │
│                                                                │       │
│                                                                │ 0..1  │
│                                                                ▼       │
│                                                           ┌──────────┐ │
│                                                           │ Penalty  │ │
│                                                           │ (흑역사)  │ │
│                                                           └──────────┘ │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                       Visualization Domain                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐      N:N      ┌───────────────┐            ┌───────────┐ │
│  │   Star   │──────────────▶│ Constellation │───────────▶│  Galaxy   │ │
│  │(별/블랙홀) │               │   (별자리)     │            │  (은하수)  │ │
│  └──────────┘               └───────────────┘            └───────────┘ │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 1. User (Aggregate Root)

```java
public class User {
    private UserId id;
    private String nickname;
    private String email;
    private Level level;
    private ExperiencePoint exp;
    private Instant createdAt;
    
    public void gainExp(int amount) {
        this.exp = this.exp.add(amount);
        checkLevelUp();
    }
    
    private void checkLevelUp() {
        while (exp.canLevelUp(level)) {
            this.level = level.next();
            // LevelUpEvent 발행
        }
    }
}

public record Level(int value) {
    public Level next() {
        return new Level(value + 1);
    }
    
    public int requiredExp() {
        return value * 100;  // 레벨 × 100 경험치 필요
    }
}

public record ExperiencePoint(long value) {
    public ExperiencePoint add(int amount) {
        return new ExperiencePoint(value + amount);
    }
    
    public boolean canLevelUp(Level currentLevel) {
        return value >= currentLevel.requiredExp();
    }
}
```

---

## 2. Session (Aggregate Root)

```java
public class Session {
    private SessionId id;
    private UserId userId;
    private StudyDayId studyDayId;
    
    // === 세션 설정 ===
    private Pledge pledge;
    private Duration targetDuration;
    private Set<Tag> tags;
    
    // === 베팅 ===
    private Bet bet;
    
    // === 상태 ===
    private SessionStatus status;
    private Instant startedAt;
    private Instant endedAt;
    
    // === 자원 ===
    private Stamina stamina;
    private FocusGauge focusGauge;
    
    // === 중단 기록 ===
    private List<Interruption> interruptions;
    
    // === 연속 집중 추적 ===
    private Instant currentFocusStartedAt;
    
    // ============================================
    //                 Factory
    // ============================================
    
    public static Session start(
            UserId userId,
            StudyDayId studyDayId,
            Pledge pledge,
            Duration targetDuration,
            Set<Tag> tags
    ) {
        Session session = new Session();
        session.id = SessionId.generate();
        session.userId = userId;
        session.studyDayId = studyDayId;
        session.pledge = pledge;
        session.targetDuration = targetDuration;
        session.tags = new HashSet<>(tags);
        session.bet = Bet.create(targetDuration, pledge.content());
        session.status = SessionStatus.ACTIVE;
        session.startedAt = Instant.now();
        session.stamina = Stamina.full();
        session.focusGauge = FocusGauge.create(targetDuration);
        session.interruptions = new ArrayList<>();
        session.currentFocusStartedAt = session.startedAt;
        
        return session;
    }
    
    // ============================================
    //                 Commands
    // ============================================
    
    /**
     * 중단 (Stop 버튼)
     */
    public Interruption pause(InterruptionReason reason) {
        validateActive();
        
        Instant now = Instant.now();
        
        // 현재까지 연속 집중 시간을 게이지에 반영
        Duration continuousFocus = Duration.between(currentFocusStartedAt, now);
        focusGauge.recordFocusPeriod(continuousFocus);
        
        this.status = SessionStatus.PAUSED;
        
        return Interruption.start(this.id, reason, now);
    }
    
    /**
     * 재개 (Resume 버튼)
     */
    public void resume(Interruption interruption) {
        validatePaused();
        
        Instant now = Instant.now();
        Duration interruptionDuration = interruption.complete(now);
        
        // 스태미나 소모
        int consumed = stamina.consume(
            interruption.getReason(),
            interruptionDuration,
            targetDuration
        );
        
        interruption.recordStaminaConsumed(consumed, stamina.getPercentage());
        this.interruptions.add(interruption);
        
        // 연속 집중 시간 리셋
        this.currentFocusStartedAt = now;
        this.status = SessionStatus.ACTIVE;
    }
    
    /**
     * 세션 완료
     */
    public SessionResult complete() {
        Instant now = Instant.now();
        
        // 마지막 연속 집중 시간 반영
        if (status == SessionStatus.ACTIVE) {
            Duration finalFocus = Duration.between(currentFocusStartedAt, now);
            focusGauge.recordFocusPeriod(finalFocus);
        }
        
        this.endedAt = now;
        this.status = SessionStatus.COMPLETED;
        
        // 베팅 심판
        Duration actualFocusTime = calculateActualFocusTime();
        BetResult betResult = judgeBet(actualFocusTime);
        
        // 경험치 계산
        int baseExp = (int) actualFocusTime.toMinutes();
        int winBonus = betResult == BetResult.WIN ? 100 : 0;
        int focusBonus = focusGauge.qualifiesForBonus() ? 50 : 0;
        
        return new SessionResult(
            this.id,
            betResult,
            actualFocusTime,
            stamina.getPercentage(),
            focusGauge.getPercentage(),
            focusGauge.getLongestContinuousFocus(),
            baseExp + winBonus + focusBonus,
            focusBonus > 0
        );
    }
    
    /**
     * 세션 포기
     */
    public SessionResult abandon() {
        Instant now = Instant.now();
        
        if (status == SessionStatus.ACTIVE) {
            Duration finalFocus = Duration.between(currentFocusStartedAt, now);
            focusGauge.recordFocusPeriod(finalFocus);
        }
        
        this.endedAt = now;
        this.status = SessionStatus.ABANDONED;
        this.bet.lose("세션 포기");
        
        Duration actualFocusTime = calculateActualFocusTime();
        int baseExp = (int) (actualFocusTime.toMinutes() * 0.5);  // 포기 시 50%만
        
        return new SessionResult(
            this.id,
            BetResult.LOSE,
            actualFocusTime,
            stamina.getPercentage(),
            focusGauge.getPercentage(),
            focusGauge.getLongestContinuousFocus(),
            baseExp,
            false
        );
    }
    
    // ============================================
    //              Private Methods
    // ============================================
    
    private BetResult judgeBet(Duration actualFocusTime) {
        boolean timeAchieved = actualFocusTime.compareTo(targetDuration) >= 0;
        boolean staminaSufficient = stamina.canWinBet();
        
        if (timeAchieved && staminaSufficient) {
            bet.win();
            return BetResult.WIN;
        } else {
            String reason = !staminaSufficient ? "스태미나 고갈" : "목표 시간 미달";
            bet.lose(reason);
            return BetResult.LOSE;
        }
    }
    
    private Duration calculateActualFocusTime() {
        Duration totalInterruption = interruptions.stream()
            .map(Interruption::getDuration)
            .reduce(Duration.ZERO, Duration::plus);
        
        Duration totalElapsed = Duration.between(startedAt, endedAt);
        return totalElapsed.minus(totalInterruption);
    }
    
    private void validateActive() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태에서만 중단할 수 있습니다.");
        }
    }
    
    private void validatePaused() {
        if (status != SessionStatus.PAUSED) {
            throw new IllegalStateException("일시정지 상태에서만 재개할 수 있습니다.");
        }
    }
}

public enum SessionStatus {
    ACTIVE,      // 집중 중
    PAUSED,      // 일시 중단
    COMPLETED,   // 정상 완료
    ABANDONED    // 포기
}
```

---

## 3. Stamina (Value Object)

```java
/**
 * 스태미나 - 세션 생존 자원
 * 
 * - 세션 시작 시 100%
 * - 중단 시 소모 (세션 시간 비례로 계산)
 * - 0%가 되면 베팅 실패 확정 (세션은 계속 가능)
 */
public class Stamina {
    private static final int MAX = 100;
    private static final int MIN_FOR_WIN = 1;
    
    private int currentValue;
    
    private Stamina(int value) {
        this.currentValue = value;
    }
    
    public static Stamina full() {
        return new Stamina(MAX);
    }
    
    /**
     * 스태미나 소모
     * 
     * 공식: 기본비용 × (중단시간 / 목표세션시간) × 100
     * → 긴 세션일수록 같은 중단에 관대함
     */
    public int consume(
            InterruptionReason reason,
            Duration interruptionDuration,
            Duration targetSessionDuration
    ) {
        double baseCost = reason.getBaseCostRatio();
        double timeFactor = (double) interruptionDuration.toSeconds() 
                          / targetSessionDuration.toSeconds();
        
        int cost = (int) Math.ceil(baseCost * timeFactor * 100);
        int actualCost = Math.min(cost, currentValue);
        
        this.currentValue = Math.max(0, currentValue - cost);
        
        return actualCost;
    }
    
    public boolean canWinBet() {
        return currentValue >= MIN_FOR_WIN;
    }
    
    public boolean isDepleted() {
        return currentValue <= 0;
    }
    
    public int getPercentage() {
        return currentValue;
    }
}
```

---

## 4. FocusGauge (Value Object)

```java
/**
 * 집중 게이지 - 연속 집중 품질 측정
 * 
 * - 세션 시작 시 0%
 * - 중단 없이 연속 집중한 최장 시간으로 계산
 * - 70% 이상 달성 시 보너스 경험치
 */
public class FocusGauge {
    private static final int BONUS_THRESHOLD = 70;
    
    private final Duration targetSessionDuration;
    private Duration longestContinuousFocus;
    
    private FocusGauge(Duration targetDuration) {
        this.targetSessionDuration = targetDuration;
        this.longestContinuousFocus = Duration.ZERO;
    }
    
    public static FocusGauge create(Duration targetDuration) {
        return new FocusGauge(targetDuration);
    }
    
    /**
     * 집중 구간 기록
     * 최장 기록보다 길면 갱신
     */
    public void recordFocusPeriod(Duration focusDuration) {
        if (focusDuration.compareTo(longestContinuousFocus) > 0) {
            this.longestContinuousFocus = focusDuration;
        }
    }
    
    /**
     * 게이지 퍼센트 = (최장 연속 집중 / 목표 시간) × 100
     * 최대 100%
     */
    public int getPercentage() {
        if (targetSessionDuration.isZero()) return 0;
        
        double ratio = (double) longestContinuousFocus.toSeconds() 
                     / targetSessionDuration.toSeconds();
        
        return (int) Math.min(100, ratio * 100);
    }
    
    public boolean qualifiesForBonus() {
        return getPercentage() >= BONUS_THRESHOLD;
    }
    
    public Duration getLongestContinuousFocus() {
        return longestContinuousFocus;
    }
}
```

---

## 5. Interruption (Entity)

```java
/**
 * 중단 기록
 */
public class Interruption {
    private InterruptionId id;
    private SessionId sessionId;
    private InterruptionReason reason;
    private Instant stoppedAt;
    private Instant resumedAt;
    private Duration duration;
    private int staminaConsumed;
    private int staminaAfter;
    
    public static Interruption start(
            SessionId sessionId,
            InterruptionReason reason,
            Instant stoppedAt
    ) {
        Interruption interruption = new Interruption();
        interruption.id = InterruptionId.generate();
        interruption.sessionId = sessionId;
        interruption.reason = reason;
        interruption.stoppedAt = stoppedAt;
        return interruption;
    }
    
    public Duration complete(Instant resumedAt) {
        this.resumedAt = resumedAt;
        this.duration = Duration.between(stoppedAt, resumedAt);
        return this.duration;
    }
    
    public void recordStaminaConsumed(int consumed, int after) {
        this.staminaConsumed = consumed;
        this.staminaAfter = after;
    }
    
    // Getters
    public InterruptionReason getReason() { return reason; }
    public Duration getDuration() { return duration; }
}
```

---

## 6. InterruptionReason (Enum)

```java
/**
 * 중단 사유
 * 
 * baseCostRatio: 기준 세션(목표시간=중단시간) 대비 소모 비율
 */
public enum InterruptionReason {
    TOILET(0.05, "화장실"),           // 가장 관대
    REST(0.10, "휴식"),
    INTERFERENCE(0.15, "외부 방해"),
    DISTRACTION(0.25, "딴짓");        // 가장 엄격
    
    private final double baseCostRatio;
    private final String displayName;
    
    InterruptionReason(double baseCostRatio, String displayName) {
        this.baseCostRatio = baseCostRatio;
        this.displayName = displayName;
    }
    
    public double getBaseCostRatio() {
        return baseCostRatio;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 7. Bet (Entity)

```java
/**
 * 베팅 - 세션과 1:1
 */
public class Bet {
    private BetId id;
    private Duration targetDuration;
    private String pledgeContent;
    private BetResult result;
    private String failReason;
    private Instant judgedAt;
    
    public static Bet create(Duration targetDuration, String pledgeContent) {
        Bet bet = new Bet();
        bet.id = BetId.generate();
        bet.targetDuration = targetDuration;
        bet.pledgeContent = pledgeContent;
        bet.result = BetResult.PENDING;
        return bet;
    }
    
    public void win() {
        this.result = BetResult.WIN;
        this.judgedAt = Instant.now();
    }
    
    public void lose(String reason) {
        this.result = BetResult.LOSE;
        this.failReason = reason;
        this.judgedAt = Instant.now();
    }
    
    // Getters
    public BetResult getResult() { return result; }
    public String getPledgeContent() { return pledgeContent; }
    public String getFailReason() { return failReason; }
}

public enum BetResult {
    PENDING,
    WIN,
    LOSE
}
```

---

## 8. SessionResult (Value Object)

```java
/**
 * 세션 완료 결과
 */
public record SessionResult(
    SessionId sessionId,
    BetResult betResult,
    Duration actualFocusTime,
    int finalStaminaPercent,
    int finalGaugePercent,
    Duration longestContinuousFocus,
    int totalExp,
    boolean receivedFocusBonus
) {
    public boolean shouldCreatePenalty() {
        return betResult == BetResult.LOSE;
    }
    
    public StarType determineStarType() {
        if (betResult == BetResult.LOSE) {
            return StarType.BLACKHOLE;
        }
        if (finalGaugePercent >= 90 && actualFocusTime.toHours() >= 2) {
            return StarType.SUPERNOVA;
        }
        return StarType.SHINING_STAR;
    }
}
```

---

## 9. Pledge (Value Object)

```java
/**
 * 다짐
 */
public record Pledge(
    String content,
    Instant createdAt
) {
    public Pledge {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("다짐 내용은 필수입니다.");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("다짐은 500자를 초과할 수 없습니다.");
        }
    }
    
    public static Pledge of(String content) {
        return new Pledge(content, Instant.now());
    }
}
```

---

## 10. Tag (Entity)

```java
/**
 * 태그 (과목) - 별의 색깔
 */
public class Tag {
    private TagId id;
    private UserId userId;
    private String name;
    private String colorHex;
    
    public static Tag create(UserId userId, String name, String colorHex) {
        Tag tag = new Tag();
        tag.id = TagId.generate();
        tag.userId = userId;
        tag.name = name;
        tag.colorHex = colorHex;
        return tag;
    }
}
```

---

## 11. StudyDay (Aggregate Root)

```java
/**
 * 하루 - 별의 원천
 */
public class StudyDay {
    private StudyDayId id;
    private UserId userId;
    private LocalDate date;
    
    // === 세션 참조 ===
    private List<SessionId> sessionIds;
    
    // === 집계 데이터 ===
    private Duration totalFocusTime;
    private int totalSessions;
    private int winCount;
    private int loseCount;
    
    // === 별 정보 ===
    private StarType starType;
    private Set<String> tagColors;
    
    // === Streak ===
    private boolean streakContinued;
    private int currentStreak;
    
    // === 하이라이트 ===
    private DailyHighlight highlight;
    
    public static StudyDay create(UserId userId, LocalDate date) {
        StudyDay day = new StudyDay();
        day.id = StudyDayId.generate();
        day.userId = userId;
        day.date = date;
        day.sessionIds = new ArrayList<>();
        day.totalFocusTime = Duration.ZERO;
        day.totalSessions = 0;
        day.winCount = 0;
        day.loseCount = 0;
        day.tagColors = new HashSet<>();
        return day;
    }
    
    public void addSessionResult(SessionResult result, Set<String> colors) {
        this.sessionIds.add(result.sessionId());
        this.totalFocusTime = totalFocusTime.plus(result.actualFocusTime());
        this.totalSessions++;
        this.tagColors.addAll(colors);
        
        if (result.betResult() == BetResult.WIN) {
            winCount++;
        } else {
            loseCount++;
        }
        
        recalculateStarType();
    }
    
    private void recalculateStarType() {
        if (loseCount > winCount) {
            this.starType = StarType.BLACKHOLE;
        } else if (totalFocusTime.toHours() >= 4 && winCount >= 3) {
            this.starType = StarType.SUPERNOVA;
        } else {
            this.starType = StarType.SHINING_STAR;
        }
    }
    
    public void finalize(DailyHighlight highlight, boolean streakContinued, int currentStreak) {
        this.highlight = highlight;
        this.streakContinued = streakContinued;
        this.currentStreak = currentStreak;
    }
}

public enum StarType {
    SHINING_STAR,   // 일반 성공
    SUPERNOVA,      // 대성공 (초신성)
    BLACKHOLE,      // 실패
    METEORITE       // 부분 실패 (차가운 운석)
}
```

---

## 12. DailyHighlight (Value Object)

```java
/**
 * 하루 하이라이트 - 경기 하이라이트처럼 편집
 */
public record DailyHighlight(
    MvpPeriod mvpPeriod,
    List<CrisisEvent> crisisEvents,
    String aiSuggestion
) {}

public record MvpPeriod(
    Instant startTime,
    Instant endTime,
    Duration duration,
    SessionId sessionId
) {
    public String toDisplayString() {
        // "오늘의 MVP 구간: 14:10~14:32 (22분 무중단)"
        return String.format("오늘의 MVP 구간: %s~%s (%d분 무중단)",
            formatTime(startTime),
            formatTime(endTime),
            duration.toMinutes()
        );
    }
}

public record CrisisEvent(
    Instant stoppedAt,
    Instant resumedAt,
    InterruptionReason reason,
    SessionId sessionId
) {
    public String toDisplayString() {
        // "위기 순간: 15:05 Stop → 15:12 Resume (유혹 이벤트 발생)"
        return String.format("위기 순간: %s Stop → %s Resume (%s)",
            formatTime(stoppedAt),
            formatTime(resumedAt),
            reason.getDisplayName()
        );
    }
}
```

---

## 13. Penalty (Aggregate Root)

```java
/**
 * 패널티 - AI가 생성한 흑역사
 */
public class Penalty {
    private PenaltyId id;
    private UserId userId;
    private SessionId sessionId;
    private BetId betId;
    
    private PenaltyType type;
    private String aiGeneratedContent;
    private PenaltyContext context;
    
    private Instant createdAt;
    private boolean isArchived;
    private boolean isViewed;
    
    public static Penalty create(
            UserId userId,
            SessionId sessionId,
            BetId betId,
            PenaltyContext context
    ) {
        Penalty penalty = new Penalty();
        penalty.id = PenaltyId.generate();
        penalty.userId = userId;
        penalty.sessionId = sessionId;
        penalty.betId = betId;
        penalty.type = PenaltyType.WEAK_HUMAN_DIARY;
        penalty.context = context;
        penalty.createdAt = Instant.now();
        penalty.isArchived = true;
        penalty.isViewed = false;
        return penalty;
    }
    
    public void generateContent(String aiContent) {
        this.aiGeneratedContent = aiContent;
    }
    
    public void markViewed() {
        this.isViewed = true;
    }
}

public enum PenaltyType {
    WEAK_HUMAN_DIARY,    // 나약한 인간의 일기
    MINOR_PUNISHMENT     // 사소한 벌칙
}

/**
 * AI 흑역사 생성용 컨텍스트
 */
public record PenaltyContext(
    String originalPledge,
    Duration targetDuration,
    Duration actualDuration,
    int finalStaminaPercent,
    int finalGaugePercent,
    String failReason,
    List<InterruptionSummary> interruptions
) {}

public record InterruptionSummary(
    InterruptionReason reason,
    Duration duration,
    int staminaConsumed
) {}
```

---

## 14. Character (Aggregate Root)

```java
/**
 * 캐릭터 - 외형 및 상태 변화
 */
public class Character {
    private CharacterId id;
    private UserId userId;
    private CharacterState state;
    private CharacterAppearance appearance;
    private Instant lastUpdatedAt;
    
    public static Character create(UserId userId) {
        Character character = new Character();
        character.id = CharacterId.generate();
        character.userId = userId;
        character.state = CharacterState.NORMAL;
        character.appearance = CharacterAppearance.initial();
        character.lastUpdatedAt = Instant.now();
        return character;
    }
    
    public void updateState(StudyDay studyDay) {
        int focusHours = (int) studyDay.getTotalFocusTime().toHours();
        int winRatio = studyDay.getWinCount() * 100 
                     / Math.max(1, studyDay.getTotalSessions());
        
        this.state = determineState(focusHours, winRatio);
        this.appearance = appearance.evolve(state);
        this.lastUpdatedAt = Instant.now();
    }
    
    private CharacterState determineState(int focusHours, int winRatio) {
        if (focusHours >= 6 && winRatio >= 80) return CharacterState.ENERGETIC;
        if (focusHours >= 4 && winRatio >= 60) return CharacterState.HAPPY;
        if (focusHours >= 2) return CharacterState.NORMAL;
        if (focusHours >= 1) return CharacterState.TIRED;
        return CharacterState.EXHAUSTED;
    }
}

public enum CharacterState {
    ENERGETIC,   // 활기참 (최상)
    HAPPY,       // 행복
    NORMAL,      // 보통
    TIRED,       // 지침
    EXHAUSTED,   // 탈진
    SLEEPING     // 잠듦
}

public record CharacterAppearance(
    String spriteKey,
    String animationState,
    Map<String, String> accessories
) {
    public static CharacterAppearance initial() {
        return new CharacterAppearance("default", "idle", Map.of());
    }
    
    public CharacterAppearance evolve(CharacterState state) {
        String newAnimation = switch (state) {
            case ENERGETIC -> "energetic_idle";
            case HAPPY -> "happy_idle";
            case NORMAL -> "idle";
            case TIRED -> "tired_idle";
            case EXHAUSTED -> "exhausted_idle";
            case SLEEPING -> "sleeping";
        };
        return new CharacterAppearance(spriteKey, newAnimation, accessories);
    }
}
```

---

## 15. 시각화 도메인 (Projection)

```java
/**
 * 별 - StudyDay의 시각화 투영 (읽기 전용)
 */
public record Star(
    LocalDate date,
    double brightness,           // 공부량 → 밝기 (0.0 ~ 1.0)
    double size,                 // 공부량 → 크기
    List<String> colors,         // 태그들의 색상
    StarType type,
    Position position,
    boolean hasNebulaEffect,     // 초신성 효과
    PenaltyId penaltyId          // 블랙홀인 경우 흑역사 참조
) {
    public static Star fromStudyDay(StudyDay day, Position position) {
        double hours = day.getTotalFocusTime().toHours();
        
        return new Star(
            day.getDate(),
            Math.min(1.0, hours / 8.0),           // 8시간 = 최대 밝기
            Math.min(50.0, 10.0 + hours * 5.0),   // 크기 계산
            new ArrayList<>(day.getTagColors()),
            day.getStarType(),
            position,
            day.getStarType() == StarType.SUPERNOVA,
            null  // 블랙홀이면 별도 조회
        );
    }
}

public record Position(double x, double y) {}

/**
 * 별자리 - 연속 Streak 시각화
 */
public record Constellation(
    List<Star> connectedStars,
    int streakDays,
    String name,
    List<Connection> connections
) {
    public record Connection(Position from, Position to) {}
}

/**
 * 은하수 - 전체 학습 기록 시각화
 */
public record Galaxy(
    UserId userId,
    List<Star> allStars,
    List<Constellation> constellations,
    GalaxyStats stats
) {}

public record GalaxyStats(
    int totalStars,
    int totalBlackholes,
    int longestStreak,
    Duration totalFocusTime
) {}
```

---

## 16. 도메인 이벤트

```java
// === 세션 이벤트 ===
public record SessionStartedEvent(
    SessionId sessionId,
    UserId userId,
    String pledge,
    Duration targetDuration,
    Set<String> tagNames,
    Instant occurredAt
) implements DomainEvent {}

public record SessionPausedEvent(
    SessionId sessionId,
    InterruptionReason reason,
    int staminaBefore,
    Instant occurredAt
) implements DomainEvent {}

public record SessionResumedEvent(
    SessionId sessionId,
    Duration interruptionDuration,
    int staminaConsumed,
    int staminaAfter,
    Instant occurredAt
) implements DomainEvent {}

public record SessionCompletedEvent(
    SessionId sessionId,
    UserId userId,
    BetResult betResult,
    Duration actualFocusTime,
    int finalStamina,
    int finalGauge,
    Duration longestContinuousFocus,
    int earnedExp,
    Instant occurredAt
) implements DomainEvent {}

// === 베팅 이벤트 ===
public record BetWonEvent(
    BetId betId,
    SessionId sessionId,
    UserId userId,
    Instant occurredAt
) implements DomainEvent {}

public record BetLostEvent(
    BetId betId,
    SessionId sessionId,
    UserId userId,
    String failReason,
    Instant occurredAt
) implements DomainEvent {}

// === 패널티 이벤트 ===
public record PenaltyCreatedEvent(
    PenaltyId penaltyId,
    UserId userId,
    SessionId sessionId,
    Instant occurredAt
) implements DomainEvent {}

public record PenaltyViewedEvent(
    PenaltyId penaltyId,
    UserId userId,
    Instant occurredAt
) implements DomainEvent {}

// === 하루 이벤트 ===
public record StudyDayFinalizedEvent(
    StudyDayId studyDayId,
    UserId userId,
    LocalDate date,
    StarType starType,
    Duration totalFocusTime,
    Instant occurredAt
) implements DomainEvent {}

// === Streak 이벤트 ===
public record StreakExtendedEvent(
    UserId userId,
    int currentStreak,
    Instant occurredAt
) implements DomainEvent {}

public record StreakBrokenEvent(
    UserId userId,
    int previousStreak,
    Instant occurredAt
) implements DomainEvent {}

// === 레벨업 이벤트 ===
public record LevelUpEvent(
    UserId userId,
    int previousLevel,
    int newLevel,
    Instant occurredAt
) implements DomainEvent {}

// === 캐릭터 이벤트 ===
public record CharacterStateChangedEvent(
    CharacterId characterId,
    UserId userId,
    CharacterState previousState,
    CharacterState newState,
    Instant occurredAt
) implements DomainEvent {}
```

---

## 17. ERD

```sql
-- =============================================
--                  사용자
-- =============================================
CREATE TABLE users (
    id UUID PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    level INT DEFAULT 1,
    experience_points BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- =============================================
--                  태그
-- =============================================
CREATE TABLE tags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(50) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, name)
);

-- =============================================
--                  하루 (별)
-- =============================================
CREATE TABLE study_days (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    total_focus_seconds BIGINT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    win_count INT DEFAULT 0,
    lose_count INT DEFAULT 0,
    star_type VARCHAR(20),
    tag_colors JSONB DEFAULT '[]',
    streak_continued BOOLEAN DEFAULT FALSE,
    current_streak INT DEFAULT 0,
    highlight_json JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, date)
);

-- =============================================
--                  세션
-- =============================================
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    study_day_id UUID NOT NULL REFERENCES study_days(id),
    
    -- 기본 정보
    pledge TEXT NOT NULL,
    target_duration_seconds BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    -- 시간
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    actual_focus_seconds BIGINT,
    
    -- 스태미나 (생존 자원, 100→0)
    stamina_initial INT DEFAULT 100,
    stamina_current INT DEFAULT 100,
    
    -- 게이지 (집중도, 0→100)
    gauge_initial INT DEFAULT 0,
    gauge_current INT DEFAULT 0,
    longest_continuous_focus_seconds BIGINT DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 세션-태그 연결
CREATE TABLE session_tags (
    session_id UUID NOT NULL REFERENCES sessions(id),
    tag_id UUID NOT NULL REFERENCES tags(id),
    PRIMARY KEY (session_id, tag_id)
);

-- =============================================
--                  중단 기록
-- =============================================
CREATE TABLE interruptions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions(id),
    reason VARCHAR(20) NOT NULL,
    stopped_at TIMESTAMP NOT NULL,
    resumed_at TIMESTAMP,
    duration_seconds INT,
    stamina_consumed INT NOT NULL,
    stamina_after INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- =============================================
--                  베팅
-- =============================================
CREATE TABLE bets (
    id UUID PRIMARY KEY,
    session_id UUID UNIQUE NOT NULL REFERENCES sessions(id),
    target_duration_seconds BIGINT NOT NULL,
    pledge_content TEXT NOT NULL,
    result VARCHAR(10),
    fail_reason VARCHAR(100),
    judged_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

-- =============================================
--                  패널티 (흑역사)
-- =============================================
CREATE TABLE penalties (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    session_id UUID NOT NULL REFERENCES sessions(id),
    bet_id UUID NOT NULL REFERENCES bets(id),
    penalty_type VARCHAR(30) NOT NULL,
    ai_generated_content TEXT,
    context_json JSONB NOT NULL,
    is_archived BOOLEAN DEFAULT TRUE,
    is_viewed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

-- =============================================
--                  캐릭터
-- =============================================
CREATE TABLE characters (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    state VARCHAR(20) NOT NULL,
    appearance_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- =============================================
--                  인덱스
-- =============================================
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_study_day_id ON sessions(study_day_id);
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_study_days_user_date ON study_days(user_id, date);
CREATE INDEX idx_interruptions_session_id ON interruptions(session_id);
CREATE INDEX idx_penalties_user_id ON penalties(user_id);
```

---

## 18. 스태미나 소모 예시표

```
┌─────────────────────────────────────────────────────────────────────┐
│                    스태미나 소모 계산표                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  공식: 기본비용 × (중단시간 / 목표세션시간) × 100                    │
│                                                                     │
│  ┌───────────┬──────────┬────────────────────────────────────────┐ │
│  │   사유     │ 기본비용  │  5분 중단 시 소모량                     │ │
│  │           │          ├──────────┬──────────┬──────────────────┤ │
│  │           │          │ 30분 세션 │ 60분 세션 │ 120분 세션       │ │
│  ├───────────┼──────────┼──────────┼──────────┼──────────────────┤ │
│  │ 화장실    │   5%     │   0.8%   │   0.4%   │   0.2%           │ │
│  │ 휴식      │  10%     │   1.7%   │   0.8%   │   0.4%           │ │
│  │ 외부 방해  │  15%     │   2.5%   │   1.3%   │   0.6%           │ │
│  │ 딴짓      │  25%     │   4.2%   │   2.1%   │   1.0%           │ │
│  └───────────┴──────────┴──────────┴──────────┴──────────────────┘ │
│                                                                     │
│  → 긴 세션일수록 같은 중단에 관대함                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 19. 세션 흐름 예시

```
┌─────────────────────────────────────────────────────────────────────┐
│                    세션 진행 예시 (목표: 60분)                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 시작                                                         │   │
│  │ • 스태미나: 100%                                             │   │
│  │ • 게이지: 0% (최장 연속: 0분)                                │   │
│  │ • 다짐: "오늘은 알고리즘 문제 3개 풀기!"                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 25분 집중 → 휴식(5분) 중단                                   │   │
│  │ • 스태미나: 100% → 99% (-0.8%)                               │   │
│  │ • 게이지: 0% → 42% (최장 연속: 25분)                         │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 15분 집중 → 딴짓(10분) 중단                                  │   │
│  │ • 스태미나: 99% → 95% (-4.2%)                                │   │
│  │ • 게이지: 42% 유지 (15분 < 25분)                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 45분 집중 → 완료!                                            │   │
│  │ • 스태미나: 95% (1% 이상 ✓)                                  │   │
│  │ • 게이지: 75% (최장 연속: 45분 갱신!)                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ AI 심판 결과                                                 │   │
│  │ ─────────────────────────────────────────────────────────── │   │
│  │ • 실제 집중: 85분 ≥ 목표 60분 ✓                              │   │
│  │ • 스태미나: 95% ≥ 1% ✓                                       │   │
│  │ • 베팅 결과: WIN! 🎉                                         │   │
│  │ ─────────────────────────────────────────────────────────── │   │
│  │ • 게이지: 75% ≥ 70% → 보너스 경험치 +50                      │   │
│  │ ─────────────────────────────────────────────────────────── │   │
│  │ 총 경험치: 85(기본) + 100(승리) + 50(보너스) = 235 EXP       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---
