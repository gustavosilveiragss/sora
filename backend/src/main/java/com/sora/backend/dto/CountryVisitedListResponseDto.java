package com.sora.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record CountryVisitedListResponseDto(
    Long userId,
    String username,
    Integer totalCountriesVisited,
    List<CountryVisitedDetailDto> countries
) {
    public record CountryVisitedDetailDto(
        CountryDto country,
        LocalDate firstVisitDate,
        LocalDate lastVisitDate,
        Integer visitCount,
        Integer postsCount,
        List<String> citiesVisited
    ) {}
}