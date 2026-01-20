package com.example.starlogue.service;

import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.AuthProvider;
import com.example.starlogue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 소셜 로그인 처리 (신규 가입 또는 기존 사용자 반환)
     */
    @Transactional
    public User loginOrRegister(AuthProvider provider, String providerId,
                                String email, String nickname, String profileImageUrl) {
        return userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseGet(() -> registerNewUser(provider, providerId, email, nickname, profileImageUrl));
    }

    /**
     * 신규 사용자 등록
     */
    @Transactional
    public User registerNewUser(AuthProvider provider, String providerId,
                                String email, String nickname, String profileImageUrl) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + email);
        }

        // 닉네임 중복 시 랜덤 접미사 추가
        String finalNickname = ensureUniqueNickname(nickname);

        User user = User.builder()
                .email(email)
                .nickname(finalNickname)
                .profileImageUrl(profileImageUrl)
                .authProvider(provider)
                .providerId(providerId)
                .build();

        return userRepository.save(user);
    }

    /**
     * 사용자 조회
     */
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * 프로필 업데이트
     */
    @Transactional
    public User updateProfile(UUID userId, String nickname, String profileImageUrl) {
        User user = getUser(userId);

        // 닉네임 변경 시 중복 체크
        if (!user.getNickname().equals(nickname) && userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다: " + nickname);
        }

        user.updateProfile(nickname, profileImageUrl);
        return user;
    }

    /**
     * 일일 성공 기록 (streak 갱신)
     */
    @Transactional
    public void recordDailySuccess(UUID userId) {
        User user = getUser(userId);
        user.recordDailySuccess();
    }

    /**
     * 일일 실패 기록 (streak 리셋)
     */
    @Transactional
    public void recordDailyFailure(UUID userId) {
        User user = getUser(userId);
        user.recordDailyFailure();
    }

    /**
     * 공부 시간 누적
     */
    @Transactional
    public void addStudyMinutes(UUID userId, int minutes) {
        User user = getUser(userId);
        user.addStudyMinutes(minutes);
    }

    /**
     * Streak 랭킹 조회
     */
    public List<User> getStreakRanking(int limit) {
        return userRepository.findTopByCurrentStreak(limit);
    }

    /**
     * 총 공부시간 랭킹 조회
     */
    public List<User> getStudyTimeRanking(int limit) {
        return userRepository.findTopByTotalStudyMinutes(limit);
    }

    // === Private Methods ===

    private String ensureUniqueNickname(String nickname) {
        String candidate = nickname;
        int suffix = 1;
        while (userRepository.existsByNickname(candidate)) {
            candidate = nickname + "_" + suffix++;
        }
        return candidate;
    }
}
