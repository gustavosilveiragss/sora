package com.sora.backend.dto;

import java.time.LocalDateTime;

public record NotificationMarkReadResponseDto(
    String message,
    NotificationDetailsDto notification
) {
    public record NotificationDetailsDto(
        Long id,
        Boolean isRead,
        LocalDateTime readAt
    ) {}
}