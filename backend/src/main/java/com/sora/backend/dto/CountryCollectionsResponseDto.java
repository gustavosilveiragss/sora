package com.sora.backend.dto;

import java.util.List;

public record CountryCollectionsResponseDto(
    Long userId,
    String username,
    Integer totalCountriesVisited,
    Integer totalCitiesVisited,
    Integer totalPostsCount,
    List<CountryCollectionResponseDto> countries
) {}