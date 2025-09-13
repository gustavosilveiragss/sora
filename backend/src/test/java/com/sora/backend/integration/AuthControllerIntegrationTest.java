package com.sora.backend.integration;

import com.sora.backend.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerUser_Success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
            "newuser",
            "newuser@email.com",
            "Password123@",
            "New",
            "User",
            "New user bio"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("newuser"))
                .andExpect(jsonPath("$.user.email").value("newuser@email.com"))
                .andExpect(jsonPath("$.user.firstName").value("New"))
                .andExpect(jsonPath("$.user.lastName").value("User"))
                .andExpect(jsonPath("$.tokens.accessToken").exists())
                .andExpect(jsonPath("$.tokens.refreshToken").exists());
    }

    @Test
    void registerUser_EmailAlreadyExists() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
            "duplicateuser",
            "test1@email.com", // Email already exists
            "Password123@",
            "Duplicate",
            "User",
            "Duplicate user bio"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_UsernameAlreadyExists() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
            "testuser1", // Username already exists
            "unique@email.com",
            "Password123@",
            "Unique",
            "User",
            "Unique user bio"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidInput() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
            "", // Empty username
            "invalid-email", // Invalid email format
            "123", // Too short password
            "",
            "",
            ""
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_Success() throws Exception {
        LoginRequestDto request = new LoginRequestDto(
            "test1@email.com",
            "Password123@"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test1@email.com"))
                .andExpect(jsonPath("$.tokens.accessToken").exists())
                .andExpect(jsonPath("$.tokens.refreshToken").exists());
    }

    @Test
    void loginUser_InvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto(
            "test1@email.com",
            "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_UserNotFound() throws Exception {
        LoginRequestDto request = new LoginRequestDto(
            "nonexistent@email.com",
            "Password123@"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_Success() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("test1@email.com", "Password123@");
        
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponseDto authResponse = objectMapper.readValue(loginResponse, AuthResponseDto.class);
        String refreshToken = authResponse.tokens().refreshToken();

        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void refreshToken_InvalidToken() throws Exception {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("invalid-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());
    }
}