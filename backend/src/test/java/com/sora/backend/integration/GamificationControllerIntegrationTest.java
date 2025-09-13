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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Gamification Controller Integration Tests")
class GamificationControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    @Transactional
    @Override
    void setUp() {
        createTestUsers();
        createEssentialTestData();
        createGamificationTestData();
    }
    
    private void createGamificationTestData() {
        createDefaultTravelPermissions();
        createGamificationTestPosts();
        createDefaultFollowRelationships();
    }

    private void createGamificationTestPosts() {
        createTestPosts();
    }

    @Test
    @DisplayName("Should get user travel statistics with correct values")
    void shouldGetUserTravelStats() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/stats")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.travelStats").exists())
                .andExpect(jsonPath("$.travelStats.totalCountriesVisited").value(4))
                .andExpect(jsonPath("$.travelStats.totalCitiesVisited").value(6))
                .andExpect(jsonPath("$.travelStats.totalPostsCount").value(6))
                .andExpect(jsonPath("$.travelStats.totalLikesReceived").value(0))
                .andExpect(jsonPath("$.travelStats.totalCommentsReceived").value(0))
                .andExpect(jsonPath("$.travelStats.totalFollowers").value(1))
                .andExpect(jsonPath("$.rankings").exists())
                .andExpect(jsonPath("$.achievements").isArray())
                .andExpect(jsonPath("$.continentStats").isArray());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user stats")
    void shouldReturn404ForNonExistentUserStats() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/999/stats")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get leaderboard with default parameters and validate rankings")
    void shouldGetLeaderboardWithDefaultParameters() throws Exception {
                
        mockMvc.perform(get("/api/gamification/leaderboard")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.metric").value("countries"))
                .andExpect(jsonPath("$.timeframe").value("all"))
                .andExpect(jsonPath("$.leaderboard").isArray())
                .andExpect(jsonPath("$.leaderboard", hasSize(2)))
                .andExpect(jsonPath("$.leaderboard[0].position").value(1))
                .andExpect(jsonPath("$.leaderboard[0].score").value(4))
                .andExpect(jsonPath("$.leaderboard[1].position").value(2))
                .andExpect(jsonPath("$.leaderboard[1].score").value(2))
                .andExpect(jsonPath("$.currentUserPosition").value(1));
    }

    @Test
    @DisplayName("Should get posts leaderboard with correct values")
    void shouldGetLeaderboardWithCustomParameters() throws Exception {
                
        mockMvc.perform(get("/api/gamification/leaderboard")
                        .param("metric", "posts")
                        .param("timeframe", "all")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.metric").value("posts"))
                .andExpect(jsonPath("$.timeframe").value("all"))
                .andExpect(jsonPath("$.leaderboard").isArray())
                .andExpect(jsonPath("$.leaderboard", hasSize(2)))
                .andExpect(jsonPath("$.leaderboard[0].position").value(1))
                .andExpect(jsonPath("$.leaderboard[0].score").value(6))
                .andExpect(jsonPath("$.leaderboard[1].position").value(2))
                .andExpect(jsonPath("$.leaderboard[1].score").value(4))
                .andExpect(jsonPath("$.currentUserPosition").value(1));
    }

    @Test
    @DisplayName("Should limit leaderboard to maximum 100")
    void shouldLimitLeaderboardToMaximum() throws Exception {
                
        mockMvc.perform(get("/api/gamification/leaderboard")
                        .param("limit", "150")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should get user rankings with correct values")
    void shouldGetUserRankings() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/rankings")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.countriesRankAmongFollowed").exists())
                .andExpect(jsonPath("$.countriesRankAmongFollowed.position").value(1))
                .andExpect(jsonPath("$.countriesRankAmongFollowed.totalUsers").value(2))
                .andExpect(jsonPath("$.countriesRankAmongFollowed.percentile", closeTo(100.0, 0.1)))
                .andExpect(jsonPath("$.postsRankAmongFollowed").exists())
                .andExpect(jsonPath("$.postsRankAmongFollowed.position").value(1))
                .andExpect(jsonPath("$.postsRankAmongFollowed.totalUsers").value(2))
                .andExpect(jsonPath("$.postsRankAmongFollowed.percentile", closeTo(100.0, 0.1)));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user rankings")
    void shouldReturn404ForNonExistentUserRankings() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/999/rankings")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get countries visited")
    void shouldGetCountriesVisited() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/countries-visited")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.totalCountriesVisited").value(4))
                .andExpect(jsonPath("$.countries").isArray())
                .andExpect(jsonPath("$.countries", hasSize(4)));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user countries visited")
    void shouldReturn404ForNonExistentUserCountriesVisited() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/999/countries-visited")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get recent destinations with default limit")
    void shouldGetRecentDestinationsWithDefaultLimit() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/recent-destinations")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.recentDestinations").isArray());
    }

    @Test
    @DisplayName("Should get recent destinations with custom limit")
    void shouldGetRecentDestinationsWithCustomLimit() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/recent-destinations")
                        .param("limit", "5")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.recentDestinations").isArray());
    }

    @Test
    @DisplayName("Should limit recent destinations to maximum 50")
    void shouldLimitRecentDestinationsToMaximum() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/recent-destinations")
                        .param("limit", "100")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent user recent destinations")
    void shouldReturn404ForNonExistentUserRecentDestinations() throws Exception {
                
        mockMvc.perform(get("/api/gamification/users/999/recent-destinations")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate leaderboard rankings order by countries")
    void shouldValidateLeaderboardRankingsOrderByCountries() throws Exception {
        mockMvc.perform(get("/api/gamification/leaderboard")
                        .param("metric", "countries")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderboard", hasSize(2)))
                .andExpect(jsonPath("$.leaderboard[0].user.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.leaderboard[0].position").value(1))
                .andExpect(jsonPath("$.leaderboard[0].score").value(4))
                .andExpect(jsonPath("$.leaderboard[1].user.id").value(testUser2.getId()))
                .andExpect(jsonPath("$.leaderboard[1].position").value(2))
                .andExpect(jsonPath("$.leaderboard[1].score").value(2));
    }

    @Test
    @DisplayName("Should validate leaderboard rankings order by posts")
    void shouldValidateLeaderboardRankingsOrderByPosts() throws Exception {
        mockMvc.perform(get("/api/gamification/leaderboard")
                        .param("metric", "posts")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderboard", hasSize(2)))
                .andExpect(jsonPath("$.leaderboard[0].user.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.leaderboard[0].position").value(1))
                .andExpect(jsonPath("$.leaderboard[0].score").value(6))
                .andExpect(jsonPath("$.leaderboard[1].user.id").value(testUser2.getId()))
                .andExpect(jsonPath("$.leaderboard[1].position").value(2))
                .andExpect(jsonPath("$.leaderboard[1].score").value(4));
    }

    @Test
    @DisplayName("Should validate user2 has lower ranking than user1")
    void shouldValidateUser2HasLowerRanking() throws Exception {
        mockMvc.perform(get("/api/gamification/users/" + testUser2.getId() + "/rankings")
                        .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countriesRankAmongFollowed.position").value(2))
                .andExpect(jsonPath("$.countriesRankAmongFollowed.totalUsers").value(2))
                .andExpect(jsonPath("$.countriesRankAmongFollowed.percentile", closeTo(50.0, 0.1)))
                .andExpect(jsonPath("$.postsRankAmongFollowed.position").value(2))
                .andExpect(jsonPath("$.postsRankAmongFollowed.totalUsers").value(2))
                .andExpect(jsonPath("$.postsRankAmongFollowed.percentile", closeTo(50.0, 0.1)));
    }

    @Test
    @DisplayName("Should validate user2 stats are accurate")
    void shouldValidateUser2Stats() throws Exception {
        mockMvc.perform(get("/api/gamification/users/" + testUser2.getId() + "/stats")
                        .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelStats.totalCountriesVisited").value(2))
                .andExpect(jsonPath("$.travelStats.totalCitiesVisited").value(4))
                .andExpect(jsonPath("$.travelStats.totalPostsCount").value(4))
                .andExpect(jsonPath("$.travelStats.totalFollowers").value(1));
    }

    @Test
    @DisplayName("Should validate user2 countries visited")
    void shouldValidateUser2CountriesVisited() throws Exception {
        mockMvc.perform(get("/api/gamification/users/" + testUser2.getId() + "/countries-visited")
                        .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountriesVisited").value(2))
                .andExpect(jsonPath("$.countries", hasSize(2)));
    }

    @Test
    @DisplayName("Should require authentication for all endpoints")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/gamification/leaderboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/rankings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/countries-visited"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/gamification/users/" + testUser1.getId() + "/recent-destinations"))
                .andExpect(status().isUnauthorized());
    }
}