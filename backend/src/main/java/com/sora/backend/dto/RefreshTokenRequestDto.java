package com.sora.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
    @NotBlank(message = "refreshToken.required")
    String refreshToken
) {}