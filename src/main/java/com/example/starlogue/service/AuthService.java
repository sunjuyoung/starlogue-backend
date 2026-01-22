package com.example.starlogue.service;

import com.example.starlogue.config.jwt.JwtTokenProvider;
import com.example.starlogue.domain.User;
import com.example.starlogue.domain.enums.AuthProvider;
import com.example.starlogue.dto.AuthDto.LoginRequest;
import com.example.starlogue.dto.AuthDto.SignUpRequest;
import com.example.starlogue.dto.AuthDto.TokenResponse;
import com.example.starlogue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(), savedUser.getEmail());

        return TokenResponse.of(accessToken, savedUser.getId(), savedUser.getEmail(), savedUser.getNickname(), savedUser.getProfileImageUrl());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다. " + user.getAuthProvider() + " 로그인을 이용해주세요");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

        return TokenResponse.of(accessToken, user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl());
    }
}
