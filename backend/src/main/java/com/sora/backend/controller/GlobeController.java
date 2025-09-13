package com.sora.backend.controller;

import com.sora.backend.dto.GlobeDataResponseDto;
import com.sora.backend.dto.PostResponseDto;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.GlobeService;
import com.sora.backend.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/globe")
@Tag(name = "Globe Interface", description = "Globe visualization data for different contexts")
public class GlobeController {

    private final GlobeService globeService;
    private final UserAccountService userAccountService;

    public GlobeController(GlobeService globeService, UserAccountService userAccountService) {
        this.globeService = globeService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/main")
    @Operation(summary = "Get main globe data", description = "Get globe data for main feed (followed users' recent posts by country)")
    @ApiResponse(responseCode = "200", description = "Main globe data retrieved successfully")
    public ResponseEntity<GlobeDataResponseDto> getMainGlobeData(Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        GlobeDataResponseDto globeData = globeService.getMainGlobeData(currentUser);
        return ResponseEntity.ok(globeData);
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile globe data", description = "Get globe data for user's travel history")
    @ApiResponse(responseCode = "200", description = "Profile globe data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<GlobeDataResponseDto> getProfileGlobeData(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        GlobeDataResponseDto globeData = globeService.getProfileGlobeData(userId);
        return ResponseEntity.ok(globeData);
    }

    @GetMapping("/explore")
    @Operation(summary = "Get explore globe data", description = "Get globe data for global content discovery")
    @ApiResponse(responseCode = "200", description = "Explore globe data retrieved successfully")
    public ResponseEntity<GlobeDataResponseDto> getExploreGlobeData(@Parameter(description = "Time period") @RequestParam(value = "timeframe", defaultValue = "month") String timeframe, @Parameter(description = "Minimum posts per country") @RequestParam(value = "minPosts", defaultValue = "5") int minPosts, Authentication authentication) {
        GlobeDataResponseDto globeData = globeService.getExploreGlobeData(timeframe, minPosts);
        return ResponseEntity.ok(globeData);
    }

    @GetMapping("/countries/{countryCode}/recent")
    @Operation(summary = "Get country recent posts", description = "Get recent posts from followed users in specific country")
    @ApiResponse(responseCode = "200", description = "Country posts retrieved successfully")
    public ResponseEntity<Page<PostResponseDto>> getCountryRecentPosts(@Parameter(description = "Country code") @PathVariable String countryCode, @Parameter(description = "Days to look back") @RequestParam(value = "days", defaultValue = "30") int days, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        days = Math.min(days, 90);
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<PostResponseDto> posts = globeService.getCountryRecentPosts(currentUser, countryCode, days, pageable);
        
        return ResponseEntity.ok(posts);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }
}