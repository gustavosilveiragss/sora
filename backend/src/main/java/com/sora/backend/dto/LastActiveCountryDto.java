package com.sora.backend.dto;

import java.time.LocalDateTime;

public record LastActiveCountryDto(
    String countryCode,
    String countryNameKey,
    LocalDateTime lastPostDate,
    Integer postsCount
) {}