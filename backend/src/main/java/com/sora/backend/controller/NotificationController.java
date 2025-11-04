package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.model.Comment;
import com.sora.backend.model.Notification;
import com.sora.backend.model.Post;
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
import java.util.List;

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
    public ResponseEntity<NotificationsResponseDto> getNotifications(@Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        Page<Notification> notifications = notificationService.getUserNotifications(currentUser.getId(), pageable);
        List<NotificationResponseDto> notificationDtos = notifications.getContent().stream()
                .map(this::mapToNotificationResponseDto)
                .toList();

        Long unreadCount = notificationService.getUnreadCount(currentUser.getId());

        NotificationsResponseDto response = new NotificationsResponseDto(
                unreadCount,
                notificationDtos,
                notifications.getNumber(),
                notifications.getTotalPages(),
                notifications.getTotalElements()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @ApiResponse(responseCode = "403", description = "Not authorized to mark this notification")
    public ResponseEntity<NotificationMarkReadResponseDto> markNotificationAsRead(@Parameter(description = "Notification ID") @PathVariable Long notificationId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Notification notification = notificationService.markAsRead(notificationId, currentUser.getId());

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
        notificationService.markAllAsRead(currentUser.getId());

        MarkAllReadResponseDto response = new MarkAllReadResponseDto(
                MessageUtil.getMessage("notifications.all.marked.read"),
                0
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Get total count of unread notifications")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public ResponseEntity<UnreadCountDto> getUnreadNotificationsCount(Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Long unreadCount = notificationService.getUnreadCount(currentUser.getId());

        UnreadCountDto response = new UnreadCountDto(unreadCount);
        return ResponseEntity.ok(response);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private NotificationResponseDto mapToNotificationResponseDto(Notification notification) {
        UserSummaryDto triggerUser = null;
        if (notification.getTriggerUser() != null) {
            UserAccount trigger = notification.getTriggerUser();
            triggerUser = new UserSummaryDto(
                    trigger.getId(),
                    trigger.getUsername(),
                    trigger.getFirstName(),
                    trigger.getLastName(),
                    trigger.getProfilePicture(),
                    null,
                    null
            );
        }

        PostSummaryDto post = null;
        if (notification.getPost() != null) {
            Post p = notification.getPost();
            UserAccount author = p.getAuthor();
            UserSummaryDto authorDto = new UserSummaryDto(
                    author.getId(),
                    author.getUsername(),
                    author.getFirstName(),
                    author.getLastName(),
                    author.getProfilePicture(),
                    null,
                    null
            );

            String thumbnailUrl = null;
            if (p.getMedia() != null && !p.getMedia().isEmpty()) {
                thumbnailUrl = p.getMedia().get(0).getCloudinaryUrl();
            }

            post = new PostSummaryDto(
                    p.getId(),
                    authorDto,
                    p.getCityName(),
                    p.getCityLatitude(),
                    p.getCityLongitude(),
                    thumbnailUrl,
                    p.getLikesCount(),
                    p.getCreatedAt()
            );
        }

        String commentPreview = null;
        if (notification.getComment() != null) {
            Comment comment = notification.getComment();
            commentPreview = comment.getContent().length() > 100
                    ? comment.getContent().substring(0, 100) + "..."
                    : comment.getContent();
        }

        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                triggerUser,
                post,
                commentPreview,
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}