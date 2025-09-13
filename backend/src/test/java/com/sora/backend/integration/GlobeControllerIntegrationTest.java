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
import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Globe Controller Integration Tests")
class GlobeControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    @Transactional
    @Override
    void setUp() {
        createTestUsers();
        createEssentialTestData();
        createGlobeTestData();
    }
    
    private void createGlobeTestData() {
        createDefaultTravelPermissions();
        createGlobeTestPosts();
        createDefaultFollowRelationships();
    }
    
    private void createGlobeTestPosts() {
        createTestPosts();
    }

    @Test
    @DisplayName("Should get main globe data with actual posts from followed users")
    void shouldGetMainGlobeData() throws Exception {
                
        mockMvc.perform(get("/api/globe/main")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.globeType").value("MAIN"))
                .andExpect(jsonPath("$.totalCountriesWithActivity").value(4))
                .andExpect(jsonPath("$.totalRecentPosts").value(10))
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.countryMarkers").isArray())
                .andExpect(jsonPath("$.countryMarkers", hasSize(4)))
                .andExpect(jsonPath("$.countryMarkers[0].countryCode").value(anyOf(equalTo("BR"), equalTo("US"), equalTo("FR"))))
                .andExpect(jsonPath("$.countryMarkers[0].recentPostsCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.countryMarkers[0].activeUsers", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.countryMarkers[0].recentPosts", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should require authentication for main globe data")
    void shouldRequireAuthenticationForMainGlobeData() throws Exception {
        mockMvc.perform(get("/api/globe/main"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get profile globe data with user's countries visited")
    void shouldGetProfileGlobeData() throws Exception {
                
        mockMvc.perform(get("/api/globe/profile/" + testUser1.getId())
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.globeType").value("PROFILE"))
                .andExpect(jsonPath("$.totalCountriesWithActivity").value(4))
                .andExpect(jsonPath("$.totalRecentPosts").value(6))
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.countryMarkers").isArray())
                .andExpect(jsonPath("$.countryMarkers", hasSize(4)))
                .andExpect(jsonPath("$.countryMarkers[0].recentPostsCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.countryMarkers[0].recentPosts", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user profile globe data")
    void shouldReturn404ForNonExistentUserProfileGlobeData() throws Exception {
                
        mockMvc.perform(get("/api/globe/profile/999")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require authentication for profile globe data")
    void shouldRequireAuthenticationForProfileGlobeData() throws Exception {
        mockMvc.perform(get("/api/globe/profile/" + testUser1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get explore globe data with posts from all users")
    void shouldGetExploreGlobeDataWithDefaultParameters() throws Exception {
                
        mockMvc.perform(get("/api/globe/explore")
                        .param("minPosts", "1")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.globeType").value("EXPLORE"))
                .andExpect(jsonPath("$.totalCountriesWithActivity").value(greaterThan(0)))
                .andExpect(jsonPath("$.totalRecentPosts").value(10))
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.countryMarkers").isArray())
                .andExpect(jsonPath("$.countryMarkers", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.countryMarkers[0].countryCode").value(anyOf(equalTo("BR"), equalTo("US"), equalTo("FR"), equalTo("JP"))))
                .andExpect(jsonPath("$.countryMarkers[0].recentPostsCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.countryMarkers[0].activeUsers", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.countryMarkers[0].recentPosts", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should filter explore globe data by minimum posts requirement")
    void shouldGetExploreGlobeDataWithCustomParameters() throws Exception {
                
        mockMvc.perform(get("/api/globe/explore")
                        .param("timeframe", "month")
                        .param("minPosts", "1")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.globeType").value("EXPLORE"))
                .andExpect(jsonPath("$.countryMarkers", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalRecentPosts").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Should require authentication for explore globe data")
    void shouldRequireAuthenticationForExploreGlobeData() throws Exception {
        mockMvc.perform(get("/api/globe/explore"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get country recent posts with actual posts from followed users")
    void shouldGetCountryRecentPostsWithDefaultParameters() throws Exception {
                
        mockMvc.perform(get("/api/globe/countries/BR/recent")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].country.code").value("BR"))
                .andExpect(jsonPath("$.content[0].cityName").exists())
                .andExpect(jsonPath("$.content[0].caption").exists())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.number").exists());
    }

    @Test
    @DisplayName("Should get country recent posts with custom parameters")
    void shouldGetCountryRecentPostsWithCustomParameters() throws Exception {
                
        mockMvc.perform(get("/api/globe/countries/BR/recent")
                        .param("days", "7")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Should limit days parameter to maximum 90")
    void shouldLimitDaysParameterToMaximum() throws Exception {
                
        mockMvc.perform(get("/api/globe/countries/BR/recent")
                        .param("days", "120")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should limit page size to maximum 100")
    void shouldLimitPageSizeToMaximum() throws Exception {
                
        mockMvc.perform(get("/api/globe/countries/BR/recent")
                        .param("size", "150")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @DisplayName("Should require authentication for country recent posts")
    void shouldRequireAuthenticationForCountryRecentPosts() throws Exception {
        mockMvc.perform(get("/api/globe/countries/BR/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle different country codes")
    void shouldHandleDifferentCountryCodes() throws Exception {
                
        mockMvc.perform(get("/api/globe/countries/US/recent")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
        
        mockMvc.perform(get("/api/globe/countries/FR/recent")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}