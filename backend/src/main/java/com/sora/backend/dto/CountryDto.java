package com.sora.backend.dto;

public record CountryDto(
    Long id,
    String code,
    String nameKey,
    Double latitude,
    Double longitude,
    String timezone
) {}