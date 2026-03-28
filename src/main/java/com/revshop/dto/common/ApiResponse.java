package com.revshop.dto.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ApiResponse<T> {

    private LocalDateTime timestamp;
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private Integer status;
    private String path;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .success(true)
                .message(message)
                .data(data)
                .errors(List.of())
                .build();
    }

    public static <T> ApiResponse<T> error(
            String message,
            Integer status,
            String path,
            List<String> errors
    ) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .success(false)
                .message(message)
                .status(status)
                .path(path)
                .errors(errors == null ? List.of() : errors)
                .build();
    }
}
