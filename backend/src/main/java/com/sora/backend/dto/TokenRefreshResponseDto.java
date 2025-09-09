package com.sora.backend.dto;

public record TokenRefreshResponseDto(
    String accessToken,
    Long expiresIn
) {}