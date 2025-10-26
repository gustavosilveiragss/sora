package com.sora.backend.integration;

import com.sora.backend.model.*;
import com.sora.backend.model.PostVisibilityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LikeControllerIntegrationTest extends BaseIntegrationTest {

    private Country testCountry;
    private Collection testCollection;
    private Post testPost;

    @BeforeEach
    void setUpLikeTests() {
        getTestCountryAndCollection();
        createTestPost();
    }

    private void getTestCountryAndCollection() {
        testCountry = countryRepository.findByCode("BR").orElseThrow(() -> 
            new RuntimeException("Test country BR should exist from BaseIntegrationTest"));
        testCollection = collectionRepository.findByCode("GENERAL").orElseThrow(() -> 
            new RuntimeException("Test collection GENERAL should exist from BaseIntegrationTest"));
    }

    private void createTestPost() {
        testPost = new Post();
        testPost.setAuthor(testUser1);
        testPost.setProfileOwner(testUser1);
        testPost.setCountry(testCountry);
        testPost.setCollection(testCollection);
        testPost.setCityName("São Paulo");
        testPost.setCityLatitude(-23.5558);
        testPost.setCityLongitude(-46.6396);
        testPost.setCaption("Test post for likes");
        testPost.setVisibilityType(PostVisibilityType.PERSONAL);
        testPost = postRepository.save(testPost);
    }

    @Test
    void likePost_Success() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likesCount").value(1))
                .andExpect(jsonPath("$.like").exists())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1))
                .andExpect(jsonPath("$.isLikedByCurrentUser").value(true));
    }

    @Test
    void likePost_AlreadyLiked_IsIdempotent() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likesCount").value(1));

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likesCount").value(1))
                .andExpect(jsonPath("$.like").exists());
    }

    @Test
    void likePost_OwnPost_IsAllowed() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likesCount").value(1))
                .andExpect(jsonPath("$.like.user.username").value("testuser1"));

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLikedByCurrentUser").value(true));
    }

    @Test
    void likePost_PostNotFound() throws Exception {
        mockMvc.perform(post("/api/posts/999999/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").exists());
    }

    @Test
    void unlikePost_Success() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.isLikedByCurrentUser").value(false));
    }

    @Test
    void unlikePost_NotLiked_IsIdempotent() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLikedByCurrentUser").value(false));
    }

    @Test
    void unlikePost_PostNotFound() throws Exception {
        mockMvc.perform(delete("/api/posts/999999/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").exists());
    }

    @Test
    void getPostLikes_Success() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/likes")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].user.username").value("testuser2"))
                .andExpect(jsonPath("$.content[0].likedAt").exists());
    }

    @Test
    void getPostLikes_EmptyList() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/likes")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getPostLikes_PostNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/999999/likes")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").exists());
    }

    @Test
    void getPostLikesCount_Success() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void getPostLikesCount_ZeroLikes() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(0));
    }

    @Test
    void likePost_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPostLikes_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/likes"))
                .andExpect(status().isUnauthorized());
    }
}