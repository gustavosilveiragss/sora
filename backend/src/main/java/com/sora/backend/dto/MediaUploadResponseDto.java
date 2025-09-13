package com.sora.backend.dto;

import java.util.List;

public record MediaUploadResponseDto(
    String message,
    List<MediaDto> media
) {}