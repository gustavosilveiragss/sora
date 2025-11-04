package com.sora.backend.dto;

import java.time.LocalDateTime;

public record PostSummaryDto(
    Long id,
    UserSummaryDto author,
    String cityName,
    Double cityLatitude,
    Double cityLongitude,
    String thumbnailUrl,
    Integer likesCount,
    LocalDateTime createdAt
) {}