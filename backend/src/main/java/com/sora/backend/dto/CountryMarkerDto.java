package com.sora.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CountryMarkerDto(
    String countryCode,
    String countryNameKey,
    Double latitude,
    Double longitude,
    Integer recentPostsCount,
    LocalDateTime lastPostDate,
    List<UserSummaryDto> activeUsers,
    List<PostSummaryDto> recentPosts
) {}