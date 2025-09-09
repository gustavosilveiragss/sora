package com.sora.backend.dto;

import java.time.LocalDateTime;

public record FollowDto(
    Long id,
    UserSummaryDto follower,
    UserSummaryDto following,
    LocalDateTime followedAt
) {}