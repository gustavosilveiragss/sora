package com.sora.backend.dto;

public record ProfilePictureResponseDto(
    String message,
    String profilePicture,
    String thumbnailUrl
) {}