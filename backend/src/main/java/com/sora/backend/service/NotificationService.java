package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Comment;
import com.sora.backend.model.Notification;
import com.sora.backend.model.NotificationType;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.NotificationRepository;
import com.sora.backend.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;


    public Notification createLikeNotification(UserAccount postAuthor, UserAccount liker, Post post) {
        if (postAuthor.getId().equals(liker.getId())) {
            log.debug("Skipping like notification - user liked own post");
            return null;
        }

        Notification notification = new Notification(postAuthor, liker, NotificationType.LIKE);
        notification.setPost(post);

        log.debug("Creating LIKE notification for user {} from user {}", postAuthor.getId(), liker.getId());
        return notificationRepository.save(notification);
    }

    public Notification createCommentNotification(UserAccount postAuthor, UserAccount commenter, Post post, Comment comment) {
        if (postAuthor.getId().equals(commenter.getId())) {
            log.debug("Skipping comment notification - user commented on own post");
            return null;
        }

        Notification notification = new Notification(postAuthor, commenter, NotificationType.COMMENT);
        notification.setPost(post);
        notification.setComment(comment);

        log.debug("Creating COMMENT notification for user {} from user {}", postAuthor.getId(), commenter.getId());
        return notificationRepository.save(notification);
    }

    public Notification createCommentReplyNotification(UserAccount originalCommenter, UserAccount replier, Post post, Comment reply) {
        if (originalCommenter.getId().equals(replier.getId())) {
            log.debug("Skipping reply notification - user replied to own comment");
            return null;
        }

        Notification notification = new Notification(originalCommenter, replier, NotificationType.COMMENT_REPLY);
        notification.setPost(post);
        notification.setComment(reply);

        log.debug("Creating COMMENT_REPLY notification for user {} from user {}", originalCommenter.getId(), replier.getId());
        return notificationRepository.save(notification);
    }

    public Notification createFollowNotification(UserAccount followedUser, UserAccount follower) {
        if (followedUser.getId().equals(follower.getId())) {
            log.debug("Skipping follow notification - user cannot follow themselves");
            return null;
        }

        Notification notification = new Notification(followedUser, follower, NotificationType.FOLLOW);

        log.debug("Creating FOLLOW notification for user {} from user {}", followedUser.getId(), follower.getId());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        log.debug("Fetching notifications for user {}", userId);
        return notificationRepository.findByRecipientId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        log.debug("Counting unread notifications for user {}", userId);
        return notificationRepository.countUnreadByRecipientId(userId);
    }

    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("notification.not.found")));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ServiceException(MessageUtil.getMessage("notification.not.authorized"));
        }

        if (notification.getIsRead()) {
            log.debug("Notification {} already marked as read", notificationId);
            return notification;
        }

        notification.markAsRead();
        log.debug("Marked notification {} as read for user {}", notificationId, userId);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        notificationRepository.markAllAsReadByRecipientId(userId);
    }
}