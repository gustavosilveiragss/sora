package com.sora.backend.dto;

public record CollectionDto(
    Long id,
    String code,
    String nameKey,
    String iconName,
    Integer sortOrder,
    Boolean isDefault
) {}