package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.exception.BusinessLogicException;
import com.sora.backend.model.UserAccount;
import com.sora.backend.security.JwtUtil;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user account with email and password")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            UserAccount user = userAccountService.registerUser(request.username(), request.email(), request.password(), request.firstName(), request.lastName(), request.bio());
            UserDetails userDetails = userAccountService.loadUserByUsername(user.getEmail());
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            UserProfileDto userProfile = createUserProfileDto(user);
            TokenResponseDto tokens = new TokenResponseDto(accessToken, refreshToken, jwtUtil.getAccessTokenExpiration());
            AuthResponseDto response = new AuthResponseDto("user.registration.success", userProfile, tokens);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(MessageUtil.getMessage("user.registration.failed"), e);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            Optional<UserAccount> userOpt = userAccountService.findByEmail(request.email());
            if (userOpt.isEmpty()) throw new BadCredentialsException(MessageUtil.getMessage("auth.invalid.credentials"));
            UserAccount user = userOpt.get();
            UserDetails userDetails = userAccountService.loadUserByUsername(user.getEmail());
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            UserProfileDto userProfile = createUserProfileDto(user);
            TokenResponseDto tokens = new TokenResponseDto(accessToken, refreshToken, jwtUtil.getAccessTokenExpiration());
            AuthResponseDto response = new AuthResponseDto("auth.login.success", userProfile, tokens);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            throw e; // Let GlobalExceptionHandler handle this properly
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(MessageUtil.getMessage("auth.login.failed"), e);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    public ResponseEntity<TokenRefreshResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        try {
            String refreshToken = request.refreshToken();
            if (!jwtUtil.validateToken(refreshToken)) throw new BusinessLogicException("auth.token.invalid", MessageUtil.getMessage("auth.token.invalid"), HttpStatus.UNAUTHORIZED);
            String tokenType = jwtUtil.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) throw new BusinessLogicException("auth.token.invalid.type", MessageUtil.getMessage("auth.token.invalid.type"), HttpStatus.UNAUTHORIZED);
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userAccountService.loadUserByUsername(username);
            String newAccessToken = jwtUtil.generateAccessToken(userDetails);
            TokenRefreshResponseDto response = new TokenRefreshResponseDto(newAccessToken, jwtUtil.getAccessTokenExpiration());
            return ResponseEntity.ok(response);
        } catch (BusinessLogicException e) {
            throw e;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(MessageUtil.getMessage("auth.token.refresh.failed"), e);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user (client should discard tokens)")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public ResponseEntity<MessageResponseDto> logout() {
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("auth.logout.success"));
        return ResponseEntity.ok(response);
    }

    private UserProfileDto createUserProfileDto(UserAccount user) {
        return new UserProfileDto(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getBio(), user.getProfilePicture(), user.getIsActive(), 0, 0, (int) userAccountService.getUserFollowersCount(user.getId()), (int) userAccountService.getUserFollowingCount(user.getId()), (int) userAccountService.getUserTotalPostsCount(user.getId()), false, user.getCreatedAt(), user.getUpdatedAt(), List.of());
    }
}