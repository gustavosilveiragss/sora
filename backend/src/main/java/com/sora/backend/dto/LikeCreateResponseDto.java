package com.sora.backend.dto;

public record LikeCreateResponseDto(
    String message,
    LikeResponseDto like,
    Integer likesCount
) {}