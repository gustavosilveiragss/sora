package com.sora.backend.integration;

import com.sora.backend.model.*;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    private Country testCountry;
    private Collection testCollection;
    private Post testPost;
    private Notification testNotification;

    @BeforeEach
    void setUpNotificationTests() {
        createTestCountryAndCollection();
        createTestPost();
        createTestNotification();
    }

    private void createTestCountryAndCollection() {
        testCountry = countryRepository.findByCode("BR").orElseThrow(
            () -> new RuntimeException("Country BR should exist from BaseIntegrationTest setup"));

        testCollection = collectionRepository.findByCode("GENERAL").orElseThrow(
            () -> new RuntimeException("Collection GENERAL should exist from BaseIntegrationTest setup"));
    }

    private void createTestPost() {
        testPost = new Post();
        testPost.setAuthor(testUser1);
        testPost.setProfileOwner(testUser1);
        testPost.setCountry(testCountry);
        testPost.setCollection(testCollection);
        testPost.setCityName("São Paulo");
        testPost.setCityLatitude(-23.5558);
        testPost.setCityLongitude(-46.6396);
        testPost.setCaption("Test post for notifications");
        testPost.setVisibilityType(PostVisibilityType.PERSONAL);
        testPost = postRepository.save(testPost);
    }

    private void createTestNotification() {
        testNotification = new Notification();
        testNotification.setRecipient(testUser1);
        testNotification.setType(NotificationType.NEW_FOLLOWER);
        testNotification.setMessage("testuser2 started following you");
        testNotification.setReferenceId(testUser2.getId().toString());
        testNotification.setIsRead(false);
        testNotification = notificationRepository.save(testNotification);
    }

    @Test
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/api/notifications")
                .param("unreadOnly", "false")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.content").isArray())
                .andExpect(jsonPath("$.notifications.totalElements").value(1))
                .andExpect(jsonPath("$.notifications.content[0].id").value(testNotification.getId()))
                .andExpect(jsonPath("$.notifications.content[0].type").value("NEW_FOLLOWER"))
                .andExpect(jsonPath("$.notifications.content[0].message").value("testuser2 started following you"))
                .andExpect(jsonPath("$.notifications.content[0].isRead").value(false))
                .andExpect(jsonPath("$.notifications.content[0].createdAt").exists());
    }

    @Test
    void getNotifications_UnreadOnly() throws Exception {
        // Create a read notification
        Notification readNotification = new Notification();
        readNotification.setRecipient(testUser1);
        readNotification.setType(NotificationType.POST_LIKED);
        readNotification.setMessage("testuser2 liked your post");
        readNotification.setReferenceId(testPost.getId().toString());
        readNotification.setIsRead(true);
        notificationRepository.save(readNotification);

        mockMvc.perform(get("/api/notifications")
                .param("unreadOnly", "true")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.content").isArray())
                .andExpect(jsonPath("$.notifications.totalElements").value(1))
                .andExpect(jsonPath("$.notifications.content[0].isRead").value(false));
    }

    @Test
    void getNotifications_FilterByType() throws Exception {
        // Create another notification with different type
        Notification commentNotification = new Notification();
        commentNotification.setRecipient(testUser1);
        commentNotification.setType(NotificationType.POST_COMMENTED);
        commentNotification.setMessage("testuser2 commented on your post");
        commentNotification.setReferenceId(testPost.getId().toString());
        commentNotification.setIsRead(false);
        notificationRepository.save(commentNotification);

        mockMvc.perform(get("/api/notifications")
                .param("type", "NEW_FOLLOWER")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.content").isArray())
                .andExpect(jsonPath("$.notifications.totalElements").value(1))
                .andExpect(jsonPath("$.notifications.content[0].type").value("NEW_FOLLOWER"));
    }

    @Test
    void getNotifications_EmptyResult() throws Exception {
        // Clear all notifications
        notificationRepository.deleteAll();

        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.content").isArray())
                .andExpect(jsonPath("$.notifications.totalElements").value(0));
    }

    @Test
    void markNotificationAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notification.isRead").value(true))
                .andExpect(jsonPath("$.message").exists());

        // Verify notification is marked as read
        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.content[0].isRead").value(true));
    }

    @Test
    void markNotificationAsRead_NotOwner() throws Exception {
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void markNotificationAsRead_NotFound() throws Exception {
        mockMvc.perform(put("/api/notifications/999999/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void markNotificationAsRead_AlreadyRead() throws Exception {
        // First mark as read
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        // Try to mark as read again
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").exists());
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        // Create additional unread notifications
        Notification notification2 = new Notification();
        notification2.setRecipient(testUser1);
        notification2.setType(NotificationType.POST_LIKED);
        notification2.setMessage("testuser2 liked your post");
        notification2.setReferenceId(testPost.getId().toString());
        notification2.setIsRead(false);
        notificationRepository.save(notification2);

        mockMvc.perform(put("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(2))
                .andExpect(jsonPath("$.message").exists());

        // Verify all notifications are marked as read
        mockMvc.perform(get("/api/notifications")
                .param("unreadOnly", "true")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.totalElements").value(0));
    }

    @Test
    void markAllAsRead_NoUnreadNotifications() throws Exception {
        // Mark existing notification as read first
        testNotification.setIsRead(true);
        notificationRepository.save(testNotification);

        mockMvc.perform(put("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(0))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        // Create additional unread notification
        Notification notification2 = new Notification();
        notification2.setRecipient(testUser1);
        notification2.setType(NotificationType.POST_LIKED);
        notification2.setMessage("testuser2 liked your post");
        notification2.setReferenceId(testPost.getId().toString());
        notification2.setIsRead(false);
        notificationRepository.save(notification2);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    void getUnreadCount_ZeroUnread() throws Exception {
        // Mark existing notification as read
        testNotification.setIsRead(true);
        notificationRepository.save(testNotification);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void getNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markNotificationAsRead_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read"))
                .andExpect(status().isUnauthorized());
    }
}