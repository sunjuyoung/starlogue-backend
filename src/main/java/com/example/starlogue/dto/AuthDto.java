package com.example.starlogue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

public class AuthDto {

    @Builder
    public record SignUpRequest(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
            String password,

            @NotBlank(message = "닉네임은 필수입니다")
            @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다")
            String nickname
    ) {}

    @Builder
    public record LoginRequest(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            String password
    ) {}

    @Builder
    public record TokenResponse(
            String accessToken,
            String tokenType,
            UUID userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
        public static TokenResponse of(String accessToken, UUID userId, String email, String nickname, String profileImageUrl) {
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .userId(userId)
                    .email(email)
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .build();
        }
    }
}
