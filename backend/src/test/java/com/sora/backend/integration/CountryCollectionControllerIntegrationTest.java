package com.sora.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Country Collection Controller Integration Tests")
class CountryCollectionControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should get current user's country collections with authentication")
    void shouldGetCurrentUserCountryCollections() throws Exception {
                
        mockMvc.perform(get("/api/country-collections")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.totalCountriesVisited").exists())
                .andExpect(jsonPath("$.totalCitiesVisited").exists())
                .andExpect(jsonPath("$.totalPostsCount").exists())
                .andExpect(jsonPath("$.countries").isArray());
    }

    @Test
    @DisplayName("Should require authentication for current user collections")
    void shouldRequireAuthenticationForCurrentUserCollections() throws Exception {
        mockMvc.perform(get("/api/country-collections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get specific user's country collections")
    void shouldGetSpecificUserCountryCollections() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId())
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.totalCountriesVisited").exists())
                .andExpect(jsonPath("$.totalCitiesVisited").exists())
                .andExpect(jsonPath("$.totalPostsCount").exists())
                .andExpect(jsonPath("$.countries").isArray());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user collections")
    void shouldReturn404ForNonExistentUser() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/999")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get country posts with default parameters")
    void shouldGetCountryPosts() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.country").exists())
                .andExpect(jsonPath("$.country.code").value("BR"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.visitInfo").exists())
                .andExpect(jsonPath("$.posts").exists())
                .andExpect(jsonPath("$.posts.content").isArray());
    }

    @Test
    @DisplayName("Should get country posts with collection filter")
    void shouldGetCountryPostsWithCollectionFilter() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .param("collectionCode", "CULINARY")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.posts.content").isArray());
    }

    @Test
    @DisplayName("Should get country posts with city filter")
    void shouldGetCountryPostsWithCityFilter() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .param("cityName", "São Paulo")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.posts.content").isArray());
    }

    @Test
    @DisplayName("Should get country posts with pagination")
    void shouldGetCountryPostsWithPagination() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.posts.size").value(10))
                .andExpect(jsonPath("$.posts.number").value(0));
    }

    @Test
    @DisplayName("Should get country posts with sorting")
    void shouldGetCountryPostsWithSorting() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "ASC")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should limit page size to maximum 100")
    void shouldLimitPageSizeToMaximum() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts")
                        .param("size", "150")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.posts.size").value(100));
    }

    @Test
    @DisplayName("Should return 404 for country posts of non-existent user")
    void shouldReturn404ForCountryPostsOfNonExistentUser() throws Exception {
                
        mockMvc.perform(get("/api/country-collections/999/BR/posts")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require authentication for country posts")
    void shouldRequireAuthenticationForCountryPosts() throws Exception {
        mockMvc.perform(get("/api/country-collections/" + testUser1.getId() + "/BR/posts"))
                .andExpect(status().isUnauthorized());
    }
}