package com.sora.backend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Location Controller Integration Tests")
class LocationControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    @Transactional
    @Override
    void setUp() {
        createTestUsers();
        createEssentialTestData();
        createLocationTestData();
    }
    
    private void createLocationTestData() {
        createDefaultTravelPermissions();
        createLocationTestPosts();
    }
    
    private void createLocationTestPosts() {
        createTestPosts();
    }

    @Test
    @DisplayName("Should get all countries ordered by name")
    void shouldGetAllCountries() throws Exception {
        mockMvc.perform(get("/api/locations/countries"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].nameKey").exists())
                .andExpect(jsonPath("$[0].latitude").exists())
                .andExpect(jsonPath("$[0].longitude").exists())
                .andExpect(jsonPath("$[0].timezone").exists());
    }

    @Test
    @DisplayName("Should search locations with valid query")
    void shouldSearchLocationsWithValidQuery() throws Exception {
        mockMvc.perform(get("/api/locations/search")
                        .param("q", "Sao Paulo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.query").value("Sao Paulo"))
                .andExpect(jsonPath("$.countryFilter").doesNotExist())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("Should search locations with country filter")
    void shouldSearchLocationsWithCountryFilter() throws Exception {
        mockMvc.perform(get("/api/locations/search")
                        .param("q", "Sao Paulo")
                        .param("countryCode", "BR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.query").value("Sao Paulo"))
                .andExpect(jsonPath("$.countryFilter").value("BR"))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("Should search locations with custom limit")
    void shouldSearchLocationsWithCustomLimit() throws Exception {
        mockMvc.perform(get("/api/locations/search")
                        .param("q", "New York")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("Should limit search results to maximum 50")
    void shouldLimitSearchResultsToMaximum() throws Exception {
        mockMvc.perform(get("/api/locations/search")
                        .param("q", "Paris")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should return 400 for query too short")
    void shouldReturn400ForQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/locations/search")
                        .param("q", "A"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for missing query")
    void shouldReturn400ForMissingQuery() throws Exception {
        mockMvc.perform(get("/api/locations/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should perform reverse geocoding with valid coordinates")
    void shouldPerformReverseGeocodingWithValidCoordinates() throws Exception {
        mockMvc.perform(get("/api/locations/reverse")
                        .param("lat", "-23.5505")
                        .param("lon", "-46.6333"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.displayName").exists())
                .andExpect(jsonPath("$.latitude").value(-23.5503898))
                .andExpect(jsonPath("$.longitude").value(-46.633081))
                .andExpect(jsonPath("$.countryCode").exists())
                .andExpect(jsonPath("$.countryName").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid latitude")
    void shouldReturn400ForInvalidLatitude() throws Exception {
        mockMvc.perform(get("/api/locations/reverse")
                        .param("lat", "95.0")
                        .param("lon", "0.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid longitude")
    void shouldReturn400ForInvalidLongitude() throws Exception {
        mockMvc.perform(get("/api/locations/reverse")
                        .param("lat", "0.0")
                        .param("lon", "185.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for missing coordinates")
    void shouldReturn400ForMissingCoordinates() throws Exception {
        mockMvc.perform(get("/api/locations/reverse")
                        .param("lat", "0.0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/locations/reverse")
                        .param("lon", "0.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should search cities in country with valid query")
    void shouldSearchCitiesInCountryWithValidQuery() throws Exception {
        mockMvc.perform(get("/api/locations/countries/BR/cities")
                        .param("q", "Sao Paulo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.query").value("Sao Paulo"))
                .andExpect(jsonPath("$.countryFilter").value("BR"))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("Should search cities in country with custom limit")
    void shouldSearchCitiesInCountryWithCustomLimit() throws Exception {
        mockMvc.perform(get("/api/locations/countries/US/cities")
                        .param("q", "New York")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should limit cities search to maximum 50")
    void shouldLimitCitiesSearchToMaximum() throws Exception {
        mockMvc.perform(get("/api/locations/countries/FR/cities")
                        .param("q", "Paris")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should return 400 for city query too short")
    void shouldReturn400ForCityQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/locations/countries/BR/cities")
                        .param("q", "A"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get popular destinations with default parameters")
    void shouldGetPopularDestinationsWithDefaultParameters() throws Exception {
        mockMvc.perform(get("/api/locations/popular"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].id").exists())
                .andExpect(jsonPath("$[*].code").exists())
                .andExpect(jsonPath("$[*].nameKey").exists());
    }

    @Test
    @DisplayName("Should get popular destinations with custom parameters")
    void shouldGetPopularDestinationsWithCustomParameters() throws Exception {
        mockMvc.perform(get("/api/locations/popular")
                        .param("limit", "5")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should limit popular destinations to maximum 50")
    void shouldLimitPopularDestinationsToMaximum() throws Exception {
        mockMvc.perform(get("/api/locations/popular")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should limit days parameter to maximum 365")
    void shouldLimitDaysParameterToMaximum() throws Exception {
        mockMvc.perform(get("/api/locations/popular")
                        .param("days", "500"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should not require authentication for location endpoints")
    void shouldNotRequireAuthenticationForLocationEndpoints() throws Exception {
        mockMvc.perform(get("/api/locations/countries"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/locations/search")
                        .param("q", "Paris"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/locations/reverse")
                        .param("lat", "0.0")
                        .param("lon", "0.0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/locations/countries/FR/cities")
                        .param("q", "Paris"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/locations/popular"))
                .andExpect(status().isOk());
    }
}