package com.sora.backend.controller;

import com.sora.backend.dto.CountryDto;
import com.sora.backend.dto.LocationSearchResponseDto;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Country;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.service.LocationService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Location Services", description = "Geographic services with OpenStreetMap integration")
public class LocationController {

    private final LocationService locationService;
    private final CountryRepository countryRepository;

    public LocationController(LocationService locationService, CountryRepository countryRepository) {
        this.locationService = locationService;
        this.countryRepository = countryRepository;
    }

    @GetMapping("/countries")
    @Operation(summary = "Get all countries", description = "Get list of all available countries")
    @ApiResponse(responseCode = "200", description = "Countries retrieved successfully")
    public ResponseEntity<List<CountryDto>> getAllCountries() {
        List<Country> countries = countryRepository.findAllByOrderByNameKeyAsc();
        List<CountryDto> countryDtos = countries.stream().map(this::mapToCountryDto).toList();
        
        return ResponseEntity.ok(countryDtos);
    }

    @GetMapping("/search")
    @Operation(summary = "Search locations", description = "Search cities and countries via OpenStreetMap")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid search query")
    public ResponseEntity<LocationSearchResponseDto> searchLocations(@Parameter(description = "Search query (min 2 chars)") @RequestParam("q") String query, @Parameter(description = "Filter by country code") @RequestParam(value = "countryCode", required = false) String countryCode, @Parameter(description = "Limit results") @RequestParam(value = "limit", defaultValue = "10") int limit) {
        if (query == null || query.trim().length() < 2) {
            throw new ServiceException(MessageUtil.getMessage("location.search.query.too.short"));
        }
        
        limit = Math.min(limit, 50);
        LocationSearchResponseDto results = locationService.searchLocations(query.trim(), countryCode, limit);
        
        return ResponseEntity.ok(results);
    }

    @GetMapping("/reverse")
    @Operation(summary = "Reverse geocoding", description = "Get location details from coordinates via OpenStreetMap")
    @ApiResponse(responseCode = "200", description = "Location details retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid coordinates")
    public ResponseEntity<LocationSearchResponseDto.LocationResultDto> reverseGeocode(@Parameter(description = "Latitude") @RequestParam("lat") Double latitude, @Parameter(description = "Longitude") @RequestParam("lon") Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ServiceException(MessageUtil.getMessage("location.coordinates.required"));
        }
        
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ServiceException(MessageUtil.getMessage("location.coordinates.invalid"));
        }
        
        LocationSearchResponseDto.LocationResultDto result = locationService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/countries/{countryCode}/cities")
    @Operation(summary = "Search cities in country", description = "Search cities within a specific country via OpenStreetMap")
    @ApiResponse(responseCode = "200", description = "Cities retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid search query or country code")
    public ResponseEntity<LocationSearchResponseDto> searchCitiesInCountry(
            @Parameter(description = "Country code") @PathVariable String countryCode,
            @Parameter(description = "City search query") @RequestParam("q") String query,
            @Parameter(description = "Limit results") @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        if (query == null || query.trim().length() < 2) {
            throw new ServiceException(MessageUtil.getMessage("location.search.query.too.short"));
        }
        
        limit = Math.min(limit, 50);
        LocationSearchResponseDto results = locationService.searchLocations(query.trim(), countryCode, limit);
        
        return ResponseEntity.ok(results);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular destinations", description = "Get popular destinations based on user posts")
    @ApiResponse(responseCode = "200", description = "Popular destinations retrieved successfully")
    public ResponseEntity<List<CountryDto>> getPopularDestinations(
            @Parameter(description = "Limit results") @RequestParam(value = "limit", defaultValue = "10") int limit,
            @Parameter(description = "Time period in days") @RequestParam(value = "days", defaultValue = "30") int days) {
        
        limit = Math.min(limit, 50);
        days = Math.min(days, 365);
        
        List<Country> popularCountries = locationService.getPopularDestinations(limit, days);
        List<CountryDto> countryDtos = popularCountries.stream().map(this::mapToCountryDto).toList();
        
        return ResponseEntity.ok(countryDtos);
    }

    private CountryDto mapToCountryDto(Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );
    }
}