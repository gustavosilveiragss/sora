package com.sora.backend.dto;

public record AuthResponseDto(
    String messageKey,
    UserProfileDto user,
    TokenResponseDto tokens
) {}