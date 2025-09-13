package com.sora.backend.integration;

import com.sora.backend.config.TestCloudinaryConfig;
import com.sora.backend.dto.UpdateProfileRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCloudinaryConfig.class)
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void getCurrentUserProfile_Success() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.username").value("testuser1"))
                .andExpect(jsonPath("$.email").value("test1@email.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User 1"))
                .andExpect(jsonPath("$.bio").value("Test bio 1"))
                .andExpect(jsonPath("$.followersCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.countriesVisitedCount").value(0));
    }

    @Test
    void getCurrentUserProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_Success() throws Exception {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
            "Test Updated",
            "User 1 Updated",
            "Updated bio for test user 1",
            "updateduser1"
        );

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test Updated"))
                .andExpect(jsonPath("$.lastName").value("User 1 Updated"))
                .andExpect(jsonPath("$.bio").value("Updated bio for test user 1"))
                .andExpect(jsonPath("$.username").value("updateduser1"));
    }

    @Test
    void updateProfile_InvalidData() throws Exception {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
            "", // Empty firstName
            "", // Empty lastName  
            "",
            "" // Empty username
        );

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_UsernameAlreadyExists() throws Exception {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
            "Test",
            "User 1",
            "Updated bio",
            "testuser2" // Username already exists
        );

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadProfilePicture_Success() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get("src/test/java/com/sora/backend/integration/paises-del-mundo.jpg"));
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "profile.jpg",
            "image/jpeg",
            imageBytes
        );

        mockMvc.perform(multipart("/api/users/profile/picture")
                .file(file)
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void uploadProfilePicture_NoFile() throws Exception {
        mockMvc.perform(multipart("/api/users/profile/picture")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_Success() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser2.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser2.getId()))
                .andExpect(jsonPath("$.username").value("testuser2"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User 2"))
                .andExpect(jsonPath("$.bio").value("Test bio 2"))
                .andExpect(jsonPath("$.email").doesNotExist()); // Email should not be exposed to other users
    }

    @Test
    void getUserById_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999999")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsers_Success() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("q", "test")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].username").exists())
                .andExpect(jsonPath("$.content[0].firstName").exists())
                .andExpect(jsonPath("$.content[0].lastName").exists());
    }

    @Test
    void searchUsers_WithCountryFilter() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("q", "test")
                .param("countryCode", "BR")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchUsers_QueryTooShort() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("q", "a") // Less than 2 characters
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followUser_Success() throws Exception {
        mockMvc.perform(post("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.follower").exists())
                .andExpect(jsonPath("$.following").exists());

        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followingCount").value(1));

        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").value(1));
    }

    @Test
    void followUser_AlreadyFollowing() throws Exception {
        // First follow
        mockMvc.perform(post("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated());

        // Try to follow again
        mockMvc.perform(post("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followUser_FollowSelf() throws Exception {
        mockMvc.perform(post("/api/users/" + testUser1.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unfollowUser_Success() throws Exception {
        mockMvc.perform(post("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followingCount").value(0));
    }

    @Test
    void unfollowUser_NotFollowing() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFollowers_Success() throws Exception {
        // Create follow relationship
        mockMvc.perform(post("/api/users/" + testUser1.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + testUser1.getId() + "/followers")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testuser2"));
    }

    @Test
    void getFollowing_Success() throws Exception {
        // Create follow relationship
        mockMvc.perform(post("/api/users/" + testUser2.getId() + "/follow")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + testUser1.getId() + "/following")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testuser2"));
    }
}