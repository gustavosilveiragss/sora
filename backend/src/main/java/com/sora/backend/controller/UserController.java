package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.FollowService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.service.UserTravelService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User profile and social features")
public class UserController {

    private final UserAccountService userAccountService;
    private final FollowService followService;
    private final UserTravelService userTravelService;

    public UserController(UserAccountService userAccountService, FollowService followService, UserTravelService userTravelService) {
        this.userAccountService = userAccountService;
        this.followService = followService;
        this.userTravelService = userTravelService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Get authenticated user's own profile with complete statistics")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(Authentication authentication) {
        UserAccount user = getCurrentUser(authentication);
        UserProfileDto profile = mapToUserProfileDto(user, user, true);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", description = "Update authenticated user's profile information")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    public ResponseEntity<UserProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        UserAccount updatedUser = userAccountService.updateProfile(currentUser.getId(), request.firstName(), request.lastName(), request.bio(), request.username());
        return ResponseEntity.ok(mapToUserProfileDto(updatedUser, currentUser, true));
    }

    @PostMapping("/profile/picture")
    @Operation(summary = "Upload profile picture", description = "Upload profile picture for authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    public ResponseEntity<ProfilePictureResponseDto> uploadProfilePicture(@RequestParam("file") MultipartFile file, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        UserAccount updatedUser = userAccountService.updateProfilePicture(currentUser.getId(), file);
        String thumbnailUrl = updatedUser.getProfilePicture() != null ? updatedUser.getProfilePicture().replace("/upload/", "/upload/c_fill,w_150,h_150/") : null;
        ProfilePictureResponseDto response = new ProfilePictureResponseDto(MessageUtil.getMessage("profile.picture.uploaded"), updatedUser.getProfilePicture(), thumbnailUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by ID", description = "Get public user profile with travel statistics and relationship status")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileDto> getUserProfile(@Parameter(description = "User ID") @PathVariable Long userId, Authentication authentication) {
        Optional<UserAccount> userOpt = userAccountService.findById(userId);
        if (userOpt.isEmpty()) throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        UserAccount user = userOpt.get();
        UserAccount currentUser = getCurrentUser(authentication);
        UserProfileDto profile = mapToUserProfileDto(user, currentUser, false);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by name/username with optional country filter and travel statistics")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Search query too short")
    public ResponseEntity<Page<UserSearchResultDto>> searchUsers(@Parameter(description = "Search query (min 2 chars)") @RequestParam("q") String query, @Parameter(description = "Filter by country code") @RequestParam(value = "countryCode", required = false) String countryCode, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        if (query == null || query.trim().length() < 2) throw new ServiceException(MessageUtil.getMessage("user.search.query.too.short"));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("username"));
        Page<UserAccount> users = userAccountService.searchUsers(query.trim(), pageable);
        UserAccount currentUser = getCurrentUser(authentication);
        Page<UserSearchResultDto> searchResults = users.map(user -> mapToUserSearchResultDto(user, currentUser));
        return ResponseEntity.ok(searchResults);
    }

    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow user", description = "Follow another user (unilateral system)")
    @ApiResponse(responseCode = "201", description = "User followed successfully")
    @ApiResponse(responseCode = "400", description = "Cannot follow yourself or already following")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<FollowDto> followUser(@Parameter(description = "User ID to follow") @PathVariable Long userId, Authentication authentication) {
        UserAccount follower = getCurrentUser(authentication);
        if (follower.getId().equals(userId)) throw new ServiceException(MessageUtil.getMessage("user.follow.cannot.follow.yourself"));
        if (!userAccountService.findById(userId).isPresent()) throw new ServiceException(MessageUtil.getMessage("user.not.found"));
        var follow = followService.followUser(follower, userId);
        return ResponseEntity.status(201).body(mapToFollowDto(follow));
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow user", description = "Stop following a user")
    @ApiResponse(responseCode = "200", description = "User unfollowed successfully")
    @ApiResponse(responseCode = "404", description = "User not found or not following")
    public ResponseEntity<MessageResponseDto> unfollowUser(@Parameter(description = "User ID to unfollow") @PathVariable Long userId, Authentication authentication) {
        UserAccount follower = getCurrentUser(authentication);
        followService.unfollowUser(follower, userId);
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("user.unfollowed.success"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get user followers", description = "Get paginated list of user's followers with relationship status")
    @ApiResponse(responseCode = "200", description = "Followers list retrieved successfully")
    public ResponseEntity<Page<UserSummaryDto>> getFollowers(@Parameter(description = "User ID") @PathVariable Long userId, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        var followers = followService.getUserFollowers(userId, pageable);
        UserAccount currentUser = getCurrentUser(authentication);
        Page<UserSummaryDto> followerDtos = followers.map(follow -> mapToUserSummaryDto(follow.getFollower(), currentUser));
        return ResponseEntity.ok(followerDtos);
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Get users being followed", description = "Get paginated list of users that this user follows")
    @ApiResponse(responseCode = "200", description = "Following list retrieved successfully")
    public ResponseEntity<Page<UserSummaryDto>> getFollowing(@Parameter(description = "User ID") @PathVariable Long userId, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        var following = followService.getUserFollowing(userId, pageable);
        UserAccount currentUser = getCurrentUser(authentication);
        Page<UserSummaryDto> followingDtos = following.map(follow -> mapToUserSummaryDto(follow.getFollowing(), currentUser));
        return ResponseEntity.ok(followingDtos);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private UserProfileDto mapToUserProfileDto(UserAccount user, UserAccount currentUser, boolean isOwnProfile) {
        var travelStats = userTravelService.getUserTravelStatistics(user.getId());
        List<LastActiveCountryDto> lastActiveCountries = userTravelService.getLastActiveCountries(user.getId(), 3);
        boolean isFollowedByCurrentUser = !isOwnProfile && followService.isUserFollowing(currentUser, user.getId());
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                isOwnProfile ? user.getEmail() : null,
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicture(),
                user.getIsActive(),
                travelStats.countriesVisitedCount(),
                travelStats.citiesVisitedCount(),
                (int) userAccountService.getUserFollowersCount(user.getId()),
                (int) userAccountService.getUserFollowingCount(user.getId()),
                travelStats.totalPostsCount(),
                isFollowedByCurrentUser,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                lastActiveCountries
        );
    }

    private UserSearchResultDto mapToUserSearchResultDto(UserAccount user, UserAccount currentUser) {
        var travelStats = userTravelService.getUserTravelStatistics(user.getId());
        boolean isFollowing = followService.isUserFollowing(currentUser, user.getId());
        List<String> commonCountries = userTravelService.getCommonCountries(currentUser.getId(), user.getId());
        LastActiveCountryDto lastActiveCountry = userTravelService.getLastActiveCountries(user.getId(), 1).stream().findFirst().orElse(null);
        return new UserSearchResultDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicture(),
                travelStats.countriesVisitedCount(),
                (int) userAccountService.getUserFollowersCount(user.getId()),
                isFollowing,
                commonCountries,
                lastActiveCountry
        );
    }

    private UserSummaryDto mapToUserSummaryDto(UserAccount user, UserAccount currentUser) {
        var travelStats = userTravelService.getUserTravelStatistics(user.getId());
        boolean isFollowing = currentUser != null && followService.isUserFollowing(currentUser, user.getId());
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                travelStats.countriesVisitedCount(),
                isFollowing
        );
    }

    private FollowDto mapToFollowDto(com.sora.backend.model.Follow follow) {
        return new FollowDto(
                follow.getId(),
                mapToUserSummaryDto(follow.getFollower(), null),
                mapToUserSummaryDto(follow.getFollowing(), null),
                follow.getCreatedAt()
        );
    }
}