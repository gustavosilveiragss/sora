package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.TravelPermissionService;
import com.sora.backend.service.UserAccountService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/travel-permissions")
@Tag(name = "Travel Permissions", description = "Country sharing and collaboration management")
public class TravelPermissionController {

    private final TravelPermissionService travelPermissionService;
    private final UserAccountService userAccountService;

    public TravelPermissionController(TravelPermissionService travelPermissionService, UserAccountService userAccountService) {
        this.travelPermissionService = travelPermissionService;
        this.userAccountService = userAccountService;
    }

    @PostMapping
    @Operation(summary = "Grant travel permission", description = "Grant permission for another user to post in your country")
    @ApiResponse(responseCode = "201", description = "Permission invitation sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or permission already exists")
    public ResponseEntity<TravelPermissionResponseDto> grantPermission(@Valid @RequestBody TravelPermissionRequestDto request, Authentication authentication) {
        UserAccount grantor = getCurrentUser(authentication);
        TravelPermission permission = travelPermissionService.grantPermission(grantor, request.granteeUsername(), request.countryCode(), request.invitationMessage());
        
        TravelPermissionResponseDto response = mapToTravelPermissionResponseDto(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{permissionId}/accept")
    @Operation(summary = "Accept travel permission", description = "Accept invitation to post in another user's country")
    @ApiResponse(responseCode = "200", description = "Permission accepted successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to accept this permission")
    @ApiResponse(responseCode = "404", description = "Permission not found")
    public ResponseEntity<Object> acceptPermission(@Parameter(description = "Permission ID") @PathVariable Long permissionId, Authentication authentication) {
        UserAccount grantee = getCurrentUser(authentication);
        TravelPermission permission = travelPermissionService.acceptPermission(permissionId, grantee);
        
        TravelPermissionResponseDto response = mapToTravelPermissionResponseDto(permission);
        
        // Create a response with both permission data and message
        var responseMap = new java.util.HashMap<String, Object>();
        responseMap.put("grantor", response.grantor());
        responseMap.put("grantee", response.grantee());
        responseMap.put("country", response.country());
        responseMap.put("status", response.status());
        responseMap.put("invitationMessage", response.invitationMessage());
        responseMap.put("createdAt", response.createdAt());
        responseMap.put("respondedAt", response.respondedAt());
        responseMap.put("collaborativePostsCount", response.collaborativePostsCount());
        responseMap.put("message", MessageUtil.getMessage("travel.permission.accepted"));
        
        return ResponseEntity.ok(responseMap);
    }

    @PostMapping("/{permissionId}/decline")
    @Operation(summary = "Decline travel permission", description = "Decline invitation to post in another user's country")
    @ApiResponse(responseCode = "200", description = "Permission declined successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to decline this permission")
    @ApiResponse(responseCode = "404", description = "Permission not found")
    public ResponseEntity<Object> declinePermission(@Parameter(description = "Permission ID") @PathVariable Long permissionId, Authentication authentication) {
        UserAccount grantee = getCurrentUser(authentication);
        TravelPermission permission = travelPermissionService.declinePermission(permissionId, grantee);
        
        TravelPermissionResponseDto response = mapToTravelPermissionResponseDto(permission);
        
        // Create a response with both permission data and message
        var responseMap = new java.util.HashMap<String, Object>();
        responseMap.put("grantor", response.grantor());
        responseMap.put("grantee", response.grantee());
        responseMap.put("country", response.country());
        responseMap.put("status", response.status());
        responseMap.put("invitationMessage", response.invitationMessage());
        responseMap.put("createdAt", response.createdAt());
        responseMap.put("respondedAt", response.respondedAt());
        responseMap.put("collaborativePostsCount", response.collaborativePostsCount());
        responseMap.put("message", MessageUtil.getMessage("travel.permission.declined"));
        
        return ResponseEntity.ok(responseMap);
    }

    @DeleteMapping("/{permissionId}")
    @Operation(summary = "Revoke travel permission", description = "Revoke previously granted permission")
    @ApiResponse(responseCode = "200", description = "Permission revoked successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to revoke this permission")
    @ApiResponse(responseCode = "404", description = "Permission not found")
    public ResponseEntity<MessageResponseDto> revokePermission(@Parameter(description = "Permission ID") @PathVariable Long permissionId, Authentication authentication) {
        UserAccount grantor = getCurrentUser(authentication);
        travelPermissionService.revokePermission(permissionId, grantor);
        
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("travel.permission.revoked"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/granted")
    @Operation(summary = "Get granted permissions", description = "Get permissions that I granted to others")
    @ApiResponse(responseCode = "200", description = "Granted permissions retrieved successfully")
    public ResponseEntity<Page<TravelPermissionResponseDto>> getGrantedPermissions(@Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) String status, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        UserAccount grantor = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        
        Page<TravelPermission> permissions = travelPermissionService.getGrantedPermissions(grantor, status, pageable);
        Page<TravelPermissionResponseDto> responses = permissions.map(this::mapToTravelPermissionResponseDto);
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/received")
    @Operation(summary = "Get received permissions", description = "Get permissions that I received from others")
    @ApiResponse(responseCode = "200", description = "Received permissions retrieved successfully")
    public ResponseEntity<Page<TravelPermissionResponseDto>> getReceivedPermissions(@Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) String status, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        UserAccount grantee = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        
        Page<TravelPermission> permissions = travelPermissionService.getReceivedPermissions(grantee, status, pageable);
        Page<TravelPermissionResponseDto> responses = permissions.map(this::mapToTravelPermissionResponseDto);
        
        return ResponseEntity.ok(responses);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private TravelPermissionResponseDto mapToTravelPermissionResponseDto(TravelPermission permission) {
        UserSummaryDto grantorDto = mapToUserSummaryDto(permission.getGrantor());
        UserSummaryDto granteeDto = mapToUserSummaryDto(permission.getGrantee());
        CountryDto countryDto = mapToCountryDto(permission.getCountry());
        
        int collaborativePostsCount = travelPermissionService.getCollaborativePostsCount(permission);
        
        return new TravelPermissionResponseDto(
                permission.getId(),
                grantorDto,
                granteeDto,
                countryDto,
                permission.getStatus(),
                permission.getInvitationMessage(),
                permission.getCreatedAt(),
                permission.getRespondedAt(),
                collaborativePostsCount
        );
    }

    private UserSummaryDto mapToUserSummaryDto(UserAccount user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                0, // countriesVisitedCount - placeholder
                false // isFollowing - placeholder
        );
    }

    private CountryDto mapToCountryDto(com.sora.backend.model.Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );
    }
}