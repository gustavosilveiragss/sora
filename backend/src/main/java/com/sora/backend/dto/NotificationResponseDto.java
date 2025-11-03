package com.sora.backend.dto;

import com.sora.backend.model.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponseDto(
    Long id,
    NotificationType type,
    UserSummaryDto triggerUser,
    PostSummaryDto post,
    String commentPreview,
    Boolean isRead,
    LocalDateTime createdAt
) {}