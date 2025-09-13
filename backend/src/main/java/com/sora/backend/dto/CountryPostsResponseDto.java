package com.sora.backend.dto;

import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;

public record CountryPostsResponseDto(
    CountryDto country,
    UserSummaryDto user,
    VisitInfoDto visitInfo,
    Page<PostResponseDto> posts
) {
    public record VisitInfoDto(
        LocalDate firstVisitDate,
        LocalDate lastVisitDate,
        Integer visitCount,
        Integer totalPostsCount,
        List<String> citiesVisited
    ) {}
}