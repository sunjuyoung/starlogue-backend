package com.example.starlogue.domain;


import com.example.starlogue.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AbstractEntity {


    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(length = 100)
    private String providerId;

    @Column(length = 100)
    private String password;  // LOCAL 인증 시 사용

    // 누적 통계 (비정규화: 조회 성능 최적화)
    @Column(nullable = false)
    private Integer totalStudyMinutes = 0;

    @Column(nullable = false)
    private Integer currentStreak = 0;  // 현재 연속 성공 일수

    @Column(nullable = false)
    private Integer longestStreak = 0;  // 최장 연속 성공 일수

    @Column(nullable = false)
    private Integer totalStars = 0;     // 총 별 개수

    @Column(nullable = false)
    private Integer totalBlackHoles = 0; // 총 블랙홀 개수

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudySession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyRecord> dailyRecords = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    @Builder
    public User(String email, String nickname, String profileImageUrl,
                AuthProvider authProvider, String providerId, String password) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.authProvider = authProvider;
        this.providerId = providerId;
        this.password = password;
    }

    // === 비즈니스 메서드 ===

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void addStudyMinutes(int minutes) {
        this.totalStudyMinutes += minutes;
    }

    public void recordDailySuccess() {
        this.currentStreak++;
        this.totalStars++;
        if (this.currentStreak > this.longestStreak) {
            this.longestStreak = this.currentStreak;
        }
    }

    public void recordDailyFailure() {
        this.currentStreak = 0;  // streak 끊김
        this.totalBlackHoles++;
    }
}