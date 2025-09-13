package com.sora.backend.dto;

import java.time.LocalDateTime;

public record LikeResponseDto(
    Long id,
    UserSummaryDto user,
    LocalDateTime likedAt
) {}