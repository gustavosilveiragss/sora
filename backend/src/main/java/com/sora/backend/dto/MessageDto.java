package com.sora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record MessageDto(
    Long id,
    @NotBlank(message = "Content cannot be blank")
    String content,
    LocalDateTime createdAt
) {
    public MessageDto(String content) {
        this(null, content, null);
    }
}