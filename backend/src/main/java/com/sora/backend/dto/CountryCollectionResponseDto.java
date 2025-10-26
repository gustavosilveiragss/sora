package com.sora.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record CountryCollectionResponseDto(
    Long countryId,
    String countryCode,
    String countryNameKey,
    Double latitude,
    Double longitude,
    LocalDate firstVisitDate,
    LocalDate lastVisitDate,
    Integer visitCount,
    Integer postsCount,
    List<String> citiesVisited,
    List<UserSummaryDto> activeCollaborators,
    Boolean hasActivePermissions,
    String latestPostImageUrl
) {}