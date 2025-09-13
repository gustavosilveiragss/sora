package com.sora.backend.dto;

public record MarkAllReadResponseDto(
    String message,
    Integer markedCount
) {}