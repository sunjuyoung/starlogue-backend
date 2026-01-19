package com.example.starlogue.domain.penalty;

import com.example.starlogue.domain.AbstractEntity;
import com.example.starlogue.domain.session.Bet;
import com.example.starlogue.domain.session.Session;
import com.example.starlogue.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "penalties",
        indexes = {
                @Index(name = "idx_penalties_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Penalty extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bet_id", nullable = false)
    private Bet bet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PenaltyType type;

    @Column(columnDefinition = "TEXT")
    private String aiGeneratedContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private PenaltyContext context;

    @Column(nullable = false)
    private boolean isArchived;

    @Column(nullable = false)
    private boolean isViewed;

    @Column(nullable = false)
    private Instant createdAt;

    private Penalty(User user, Session session, Bet bet, PenaltyContext context) {
        this.user = user;
        this.session = session;
        this.bet = bet;
        this.type = PenaltyType.WEAK_HUMAN_DIARY;
        this.context = context;
        this.isArchived = true;
        this.isViewed = false;
        this.createdAt = Instant.now();
    }

    public static Penalty create(User user, Session session, Bet bet, PenaltyContext context) {
        return new Penalty(user, session, bet, context);
    }

    public void generateContent(String aiContent) {
        this.aiGeneratedContent = aiContent;
    }

    public void markViewed() {
        this.isViewed = true;
    }

    public void unarchive() {
        this.isArchived = false;
    }
}
