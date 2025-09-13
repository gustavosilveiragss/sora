package com.sora.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sora.backend.dto.LocationSearchResponseDto;
import com.sora.backend.model.Country;
import com.sora.backend.model.Post;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class LocationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    private final RestTemplate restTemplate;
    private final CountryRepository countryRepository;
    private final PostRepository postRepository;

    public LocationService(RestTemplate restTemplate, CountryRepository countryRepository, PostRepository postRepository) {
        this.restTemplate = restTemplate;
        this.countryRepository = countryRepository;
        this.postRepository = postRepository;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimResponse {
        @JsonProperty("place_id")
        private String placeId;
        
        @JsonProperty("licence")
        private String licence;
        
        @JsonProperty("osm_type")
        private String osmType;
        
        @JsonProperty("osm_id")
        private String osmId;
        
        @JsonProperty("lat")
        private String lat;
        
        @JsonProperty("lon")
        private String lon;
        
        @JsonProperty("class")
        private String clazz;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("place_rank")
        private Integer placeRank;
        
        @JsonProperty("importance")
        private Double importance;
        
        @JsonProperty("addresstype")
        private String addressType;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("display_name")
        private String displayName;
        
        @JsonProperty("address")
        private NominatimAddress address;

        public String getPlaceId() { return placeId; }
        public void setPlaceId(String placeId) { this.placeId = placeId; }
        public String getOsmType() { return osmType; }
        public void setOsmType(String osmType) { this.osmType = osmType; }
        public String getLat() { return lat; }
        public void setLat(String lat) { this.lat = lat; }
        public String getLon() { return lon; }
        public void setLon(String lon) { this.lon = lon; }
        public Double getImportance() { return importance; }
        public void setImportance(Double importance) { this.importance = importance; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public NominatimAddress getAddress() { return address; }
        public void setAddress(NominatimAddress address) { this.address = address; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimAddress {
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("town")
        private String town;
        
        @JsonProperty("village")
        private String village;
        
        @JsonProperty("state")
        private String state;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("country_code")
        private String countryCode;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getTown() { return town; }
        public void setTown(String town) { this.town = town; }
        public String getVillage() { return village; }
        public void setVillage(String village) { this.village = village; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }

    public LocationSearchResponseDto searchLocations(String query, String countryCode, int limit) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString("https://nominatim.openstreetmap.org/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", Math.min(limit, 50))
                    .queryParam("accept-language", "en");

            if (countryCode != null) {
                builder.queryParam("countrycodes", countryCode.toLowerCase());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SoraApp/1.0 (contact@sora.app)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    NominatimResponse[].class
            );

            if (response.getBody() != null) {
                List<LocationSearchResponseDto.LocationResultDto> results = Stream.of(response.getBody())
                        .map(this::mapToLocationResult)
                        .collect(Collectors.toList());

                return new LocationSearchResponseDto(query, countryCode, results);
            }
        } catch (Exception e) {
            logger.error("OpenStreetMap search failed for query '{}': {}", query, e.getMessage());
        }

        return new LocationSearchResponseDto(query, countryCode, List.of());
    }

    public LocationSearchResponseDto.LocationResultDto reverseGeocode(Double latitude, Double longitude) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://nominatim.openstreetmap.org/reverse")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("accept-language", "en")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SoraApp/1.0 (contact@sora.app)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NominatimResponse.class
            );

            if (response.getBody() != null) {
                return mapToLocationResult(response.getBody());
            }
        } catch (Exception e) {
            logger.error("OpenStreetMap reverse geocode failed for coordinates ({}, {}): {}", latitude, longitude, e.getMessage());
        }

        return new LocationSearchResponseDto.LocationResultDto(
                "Unknown Location",
                "Unknown City",
                "Unknown State",
                "XX",
                "Unknown Country",
                latitude,
                longitude,
                0.5,
                "city"
        );
    }

    public List<Country> getPopularDestinations(int limit, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.findMostPopularCountriesByPosts(since, PageRequest.of(0, limit));
    }

    private LocationSearchResponseDto.LocationResultDto mapToLocationResult(NominatimResponse response) {
        String cityName = getCityName(response.getAddress());
        String stateName = response.getAddress() != null ? response.getAddress().getState() : null;
        String countryName = response.getAddress() != null ? response.getAddress().getCountry() : null;
        String countryCode = response.getAddress() != null ? 
                (response.getAddress().getCountryCode() != null ? response.getAddress().getCountryCode().toUpperCase() : null) : null;

        return new LocationSearchResponseDto.LocationResultDto(
                response.getDisplayName() != null ? response.getDisplayName() : response.getName(),
                cityName,
                stateName,
                countryCode,
                countryName,
                response.getLat() != null ? Double.parseDouble(response.getLat()) : 0.0,
                response.getLon() != null ? Double.parseDouble(response.getLon()) : 0.0,
                response.getImportance() != null ? response.getImportance() : 0.5,
                response.getOsmType() != null ? response.getOsmType() : "unknown"
        );
    }

    private String getCityName(NominatimAddress address) {
        if (address == null) return null;
        if (address.getCity() != null) return address.getCity();
        if (address.getTown() != null) return address.getTown();
        if (address.getVillage() != null) return address.getVillage();
        return null;
    }
}