package com.sora.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponseDto(
    Long id,
    UserSummaryDto author,
    String content,
    Integer repliesCount,
    List<CommentResponseDto> replies,
    Boolean isLikedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}