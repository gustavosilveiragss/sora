package com.sora.backend.dto;

import java.util.List;

public record NotificationsResponseDto(
    Long unreadCount,
    List<NotificationResponseDto> notifications,
    Integer currentPage,
    Integer totalPages,
    Long totalElements
) {}