package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Country;
import com.sora.backend.model.Notification;
import com.sora.backend.model.NotificationType;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;


    public Notification createFollowNotification(UserAccount recipient, UserAccount follower) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.NEW_FOLLOWER);
        notification.setMessage(MessageUtil.getMessage("notification.user.followed", follower.getUsername()));
        notification.setReferenceId(follower.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    public Notification createPostLikedNotification(UserAccount recipient, UserAccount liker, Post post) {
        if (recipient.getId().equals(liker.getId())) {
            return null;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.POST_LIKED);
        notification.setMessage(MessageUtil.getMessage("notification.post.liked", liker.getUsername()));
        notification.setReferenceId(post.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createPostCommentedNotification(UserAccount recipient, UserAccount commenter, Post post) {
        if (recipient.getId().equals(commenter.getId())) {
            return null;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.POST_COMMENTED);
        notification.setMessage(MessageUtil.getMessage("notification.post.commented", commenter.getUsername()));
        notification.setReferenceId(post.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createTravelPermissionInvitation(UserAccount recipient, UserAccount grantor, 
                                                       Country country, Long permissionId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TRAVEL_PERMISSION_INVITATION);
        notification.setMessage(MessageUtil.getMessage("notification.travel.permission.invitation", grantor.getUsername(), MessageUtil.getMessage(country.getNameKey())));
        notification.setReferenceId(permissionId.toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createTravelPermissionAccepted(UserAccount recipient, UserAccount grantee, Country country) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TRAVEL_PERMISSION_ACCEPTED);
        notification.setMessage(MessageUtil.getMessage("notification.travel.permission.accepted", grantee.getUsername(), MessageUtil.getMessage(country.getNameKey())));
        notification.setReferenceId(country.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createTravelPermissionDeclined(UserAccount recipient, UserAccount grantee, Country country) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TRAVEL_PERMISSION_DECLINED);
        notification.setMessage(MessageUtil.getMessage("notification.travel.permission.declined", grantee.getUsername(), MessageUtil.getMessage(country.getNameKey())));
        notification.setReferenceId(country.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createTravelPermissionRevoked(UserAccount recipient, UserAccount grantor, Country country) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TRAVEL_PERMISSION_REVOKED);
        notification.setMessage(MessageUtil.getMessage("notification.travel.permission.revoked", grantor.getUsername(), MessageUtil.getMessage(country.getNameKey())));
        notification.setReferenceId(country.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    public Notification createCommentRepliedNotification(UserAccount recipient, UserAccount replier, Long commentId) {
        if (recipient.getId().equals(replier.getId())) {
            return null;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.COMMENT_REPLIED);
        notification.setMessage(MessageUtil.getMessage("notification.comment.replied", replier.getUsername()));
        notification.setReferenceId(commentId.toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UserAccount user, Boolean unreadOnly, Pageable pageable) {
        if (unreadOnly != null && unreadOnly)
            return notificationRepository.findByRecipientIdAndIsRead(user.getId(), false, pageable);
        return notificationRepository.findByRecipientId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UserAccount user, boolean unreadOnly, NotificationType notificationType, Pageable pageable) {
        if (notificationType != null) {
            if (unreadOnly) {
                return notificationRepository.findByRecipientIdAndIsReadAndType(user.getId(), false, notificationType, pageable);
            } else {
                return notificationRepository.findByRecipientIdAndType(user.getId(), notificationType, pageable);
            }
        } else {
            return getUserNotifications(user, unreadOnly, pageable);
        }
    }

    @Transactional(readOnly = true)
    public com.fasterxml.jackson.databind.JsonNode getNotificationData(Notification notification) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode data = mapper.createObjectNode();
        data.put("type", notification.getType().name());
        data.put("referenceId", notification.getReferenceId());
        return data;
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount(UserAccount user) {
        return notificationRepository.countByRecipientIdAndIsRead(user.getId(), false);
    }

    public Notification markAsRead(Long notificationId, UserAccount user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("notification.not.found")));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ServiceException(MessageUtil.getMessage("notification.not.authorized"));
        }
        
        if (notification.getIsRead()) {
            throw new ServiceException(MessageUtil.getMessage("notification.already.read"));
        }

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    public long markAllAsRead(UserAccount user) {
        Page<Notification> unreadNotifications = notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(user.getId(), false, Pageable.unpaged());

        long markedCount = 0;
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            markedCount++;
        }

        return markedCount;
    }

    @Transactional
    public void cleanupOldNotifications(UserAccount user, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteByRecipientIdAndCreatedAtBefore(user.getId(), cutoffDate);
    }
}