package com.sora.backend.dto;

import org.springframework.data.domain.Page;

public record NotificationsResponseDto(
    Integer unreadCount,
    Page<NotificationResponseDto> notifications
) {}