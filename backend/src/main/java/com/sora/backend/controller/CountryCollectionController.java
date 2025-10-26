package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.PostService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.service.UserTravelService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/country-collections")
@Tag(name = "Country Collections", description = "User travel collections and country-based posts")
public class CountryCollectionController {
    private final UserTravelService userTravelService;
    private final PostService postService;
    private final UserAccountService userAccountService;

    public CountryCollectionController(UserTravelService userTravelService, PostService postService, UserAccountService userAccountService) {
        this.userTravelService = userTravelService;
        this.postService = postService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    @Operation(summary = "Get user's country collections", description = "Get authenticated user's country collections with travel statistics")
    @ApiResponse(responseCode = "200", description = "Country collections retrieved successfully")
    public ResponseEntity<CountryCollectionsResponseDto> getCurrentUserCountryCollections(Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        CountryCollectionsResponseDto collections = userTravelService.getUserCountryCollections(currentUser.getId());
        
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user's country collections", description = "Get specific user's public country collections")
    @ApiResponse(responseCode = "200", description = "Country collections retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<CountryCollectionsResponseDto> getUserCountryCollections(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }
        
        CountryCollectionsResponseDto collections = userTravelService.getUserCountryCollections(userId);
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/{userId}/{countryCode}/posts")
    @Operation(summary = "Get country posts", description = "Get posts from a specific country collection")
    @ApiResponse(responseCode = "200", description = "Country posts retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User or country not found")
    public ResponseEntity<CountryPostsResponseDto> getCountryPosts(@Parameter(description = "User ID") @PathVariable Long userId, @Parameter(description = "Country code") @PathVariable String countryCode, @Parameter(description = "Filter by collection") @RequestParam(value = "collectionCode", required = false) String collectionCode, @Parameter(description = "Filter by city") @RequestParam(value = "cityName", required = false) String cityName, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, @Parameter(description = "Sort by") @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy, @Parameter(description = "Sort direction") @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }

        UserAccount user = userOpt.get();
        UserAccount currentUser = getCurrentUser(authentication);
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortBy));

        CountryPostsResponseDto response = postService.getCountryPosts(userId, countryCode, collectionCode, cityName, pageable, currentUser);

        return ResponseEntity.ok(response);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }
}