package com.example.starlogue.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 래퍼
 *
 * 모든 API 응답을 일관된 형식으로 감싸서 반환합니다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    // === 성공 응답 ===

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }

    // === 실패 응답 ===

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now());
    }
}