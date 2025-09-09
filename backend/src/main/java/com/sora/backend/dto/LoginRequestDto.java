package com.sora.backend.dto;

import com.sora.backend.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @ValidEmail
    @NotBlank(message = "email.required")
    String email,
    
    @NotBlank(message = "password.required")
    String password
) {}