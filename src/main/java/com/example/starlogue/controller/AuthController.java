package com.example.starlogue.controller;

import com.example.starlogue.controller.response.ApiResponse;
import com.example.starlogue.dto.AuthDto.LoginRequest;
import com.example.starlogue.dto.AuthDto.SignUpRequest;
import com.example.starlogue.dto.AuthDto.TokenResponse;
import com.example.starlogue.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        TokenResponse response = authService.signUp(request);
        return ApiResponse.ok(response, "회원가입이 완료되었습니다");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ApiResponse.ok(response, "로그인되었습니다");
    }
}
