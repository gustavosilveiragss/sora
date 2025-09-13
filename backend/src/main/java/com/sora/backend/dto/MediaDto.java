package com.sora.backend.dto;

import com.sora.backend.model.MediaType;
import java.time.LocalDateTime;

public record MediaDto(
    Long id,
    String fileName,
    String cloudinaryPublicId,
    String cloudinaryUrl,
    String thumbnailUrl,
    MediaType mediaType,
    Long fileSize,
    Integer width,
    Integer height,
    Integer sortOrder,
    LocalDateTime uploadedAt
) {}