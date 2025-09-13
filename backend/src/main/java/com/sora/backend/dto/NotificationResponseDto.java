package com.sora.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sora.backend.model.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponseDto(
    Long id,
    NotificationType type,
    String message,
    String referenceId,
    Boolean isRead,
    LocalDateTime createdAt,
    JsonNode data
) {}