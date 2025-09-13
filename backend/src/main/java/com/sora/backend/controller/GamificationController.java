package com.sora.backend.controller;

import com.sora.backend.dto.LeaderboardResponseDto;
import com.sora.backend.dto.UserGamificationStatsResponseDto;
import com.sora.backend.dto.CountryVisitedListResponseDto;
import com.sora.backend.dto.RecentDestinationsResponseDto;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.GamificationService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/gamification")
@Tag(name = "Gamification", description = "Travel statistics, rankings and achievements")
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserAccountService userAccountService;

    public GamificationController(GamificationService gamificationService, UserAccountService userAccountService) {
        this.gamificationService = gamificationService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/users/{userId}/stats")
    @Operation(summary = "Get user travel statistics", description = "Get comprehensive travel statistics and achievements for a user")
    @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserGamificationStatsResponseDto> getUserTravelStats(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }
        
        UserGamificationStatsResponseDto stats = gamificationService.getUserTravelStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get travel leaderboard", description = "Get ranking of users based on travel metrics among followed users")
    @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    public ResponseEntity<LeaderboardResponseDto> getLeaderboard(@Parameter(description = "Ranking metric") @RequestParam(value = "metric", defaultValue = "countries") String metric, @Parameter(description = "Time period") @RequestParam(value = "timeframe", defaultValue = "all") String timeframe, @Parameter(description = "Limit results") @RequestParam(value = "limit", defaultValue = "20") int limit, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        limit = Math.min(limit, 100);
        
        LeaderboardResponseDto leaderboard = gamificationService.getLeaderboard(currentUser, metric, timeframe, limit);
        
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/users/{userId}/rankings")
    @Operation(summary = "Get user rankings", description = "Get user's position in various rankings among followed users")
    @ApiResponse(responseCode = "200", description = "User rankings retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserGamificationStatsResponseDto.RankingsDto> getUserRankings(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }
        
        UserAccount currentUser = getCurrentUser(authentication);
        UserGamificationStatsResponseDto.RankingsDto rankings = gamificationService.getUserRankings(userId, currentUser);
        
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/users/{userId}/countries-visited")
    @Operation(summary = "Get countries visited", description = "Get detailed list of countries visited by user")
    @ApiResponse(responseCode = "200", description = "Countries visited retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<CountryVisitedListResponseDto> getCountriesVisited(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }
        
        CountryVisitedListResponseDto countries = gamificationService.getCountriesVisited(userId);
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/users/{userId}/recent-destinations")
    @Operation(summary = "Get recent destinations", description = "Get user's most recent travel destinations")
    @ApiResponse(responseCode = "200", description = "Recent destinations retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<RecentDestinationsResponseDto> getRecentDestinations(@Parameter(description = "User ID") @PathVariable Long userId, @Parameter(description = "Limit results") @RequestParam(value = "limit", defaultValue = "10") int limit, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        }
        
        limit = Math.min(limit, 50);
        RecentDestinationsResponseDto destinations = gamificationService.getRecentDestinations(userId, limit);
        
        return ResponseEntity.ok(destinations);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }
}