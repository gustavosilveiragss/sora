package com.sora.backend.dto;

import com.sora.backend.model.PostVisibilityType;
import java.time.LocalDateTime;
import java.util.List;

public record PostResponseDto(
    Long id,
    UserSummaryDto author,
    UserSummaryDto profileOwner,
    CountryDto country,
    CollectionDto collection,
    String cityName,
    Double cityLatitude,
    Double cityLongitude,
    String caption,
    List<MediaDto> media,
    Integer likesCount,
    Integer commentsCount,
    Boolean isLikedByCurrentUser,
    PostVisibilityType visibilityType,
    String sharedPostGroupId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}