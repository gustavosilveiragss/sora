package com.sora.backend.dto;

import com.sora.backend.model.TravelPermissionStatus;
import java.time.LocalDateTime;

public record TravelPermissionResponseDto(
    Long id,
    UserSummaryDto grantor,
    UserSummaryDto grantee,
    CountryDto country,
    TravelPermissionStatus status,
    String invitationMessage,
    LocalDateTime createdAt,
    LocalDateTime respondedAt,
    Integer collaborativePostsCount
) {}