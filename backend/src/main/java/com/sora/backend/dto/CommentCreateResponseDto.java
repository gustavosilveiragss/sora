package com.sora.backend.dto;

public record CommentCreateResponseDto(
    String message,
    CommentResponseDto comment
) {}