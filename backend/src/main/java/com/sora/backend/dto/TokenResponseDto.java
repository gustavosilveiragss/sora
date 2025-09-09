package com.sora.backend.dto;

public record TokenResponseDto(
    String accessToken,
    String refreshToken,
    Long expiresIn
) {}