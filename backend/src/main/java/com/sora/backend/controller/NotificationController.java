package com.sora.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sora.backend.dto.*;
import com.sora.backend.model.Notification;
import com.sora.backend.model.NotificationType;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.NotificationService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "User notifications management")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserAccountService userAccountService;

    public NotificationController(NotificationService notificationService, UserAccountService userAccountService) {
        this.notificationService = notificationService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get paginated list of user notifications")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    public ResponseEntity<NotificationsResponseDto> getNotifications(@Parameter(description = "Only unread notifications") @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly, @Parameter(description = "Filter by notification type") @RequestParam(value = "type", required = false) String type, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        
        NotificationType notificationType = null;
        if (type != null) {
            try {
                notificationType = NotificationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid type, ignore filter
            }
        }
        
        Page<Notification> notifications = notificationService.getUserNotifications(currentUser, unreadOnly, notificationType, pageable);
        
        Page<NotificationResponseDto> notificationDtos = notifications.map(this::mapToNotificationResponseDto);
        
        int unreadCount = (int) notificationService.getUnreadNotificationsCount(currentUser);
        
        NotificationsResponseDto response = new NotificationsResponseDto(unreadCount, notificationDtos);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @ApiResponse(responseCode = "403", description = "Not authorized to mark this notification")
    public ResponseEntity<NotificationMarkReadResponseDto> markNotificationAsRead(@Parameter(description = "Notification ID") @PathVariable Long notificationId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Notification notification = notificationService.markAsRead(notificationId, currentUser);
        
        NotificationMarkReadResponseDto.NotificationDetailsDto details = 
                new NotificationMarkReadResponseDto.NotificationDetailsDto(
                        notification.getId(),
                        notification.getIsRead(),
                        LocalDateTime.now()
                );
        
        NotificationMarkReadResponseDto response = new NotificationMarkReadResponseDto(
                MessageUtil.getMessage("notification.marked.read"),
                details
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read", description = "Mark all user notifications as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully")
    public ResponseEntity<MarkAllReadResponseDto> markAllNotificationsAsRead(Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        int markedCount = (int) notificationService.markAllAsRead(currentUser);
        
        MarkAllReadResponseDto response = new MarkAllReadResponseDto(
                MessageUtil.getMessage("notifications.all.marked.read"),
                markedCount
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Get total count of unread notifications")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public ResponseEntity<UnreadCountResponseDto> getUnreadNotificationsCount(Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        int unreadCount = (int) notificationService.getUnreadNotificationsCount(currentUser);
        
        UnreadCountResponseDto response = new UnreadCountResponseDto(unreadCount);
        return ResponseEntity.ok(response);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private NotificationResponseDto mapToNotificationResponseDto(Notification notification) {
        JsonNode data = notificationService.getNotificationData(notification);
        
        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                data
        );
    }
}