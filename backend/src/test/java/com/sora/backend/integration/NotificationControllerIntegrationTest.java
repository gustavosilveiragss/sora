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
        testNotification = new Notification(testUser1, testUser2, NotificationType.FOLLOW);
        testNotification = notificationRepository.save(testNotification);
    }

    @Test
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(testNotification.getId()))
                .andExpect(jsonPath("$.notifications[0].type").value("FOLLOW"))
                .andExpect(jsonPath("$.notifications[0].triggerUser.id").value(testUser2.getId()))
                .andExpect(jsonPath("$.notifications[0].triggerUser.username").value(testUser2.getUsername()))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.notifications[0].createdAt").exists());
    }

    @Test
    void getNotifications_MultipleNotifications() throws Exception {
        Notification likeNotification = new Notification(testUser1, testUser2, NotificationType.LIKE);
        likeNotification.setPost(testPost);
        likeNotification.setIsRead(true);
        notificationRepository.save(likeNotification);

        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getNotifications_WithPost() throws Exception {
        Notification likeNotification = new Notification(testUser1, testUser2, NotificationType.LIKE);
        likeNotification.setPost(testPost);
        notificationRepository.save(likeNotification);

        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[?(@.type=='LIKE')].post").exists())
                .andExpect(jsonPath("$.notifications[?(@.type=='LIKE')].post.id").value(testPost.getId().intValue()))
                .andExpect(jsonPath("$.notifications[?(@.type=='LIKE')].post.cityName").value("São Paulo"));
    }

    @Test
    void getNotifications_EmptyResult() throws Exception {
        notificationRepository.deleteAll();

        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void markNotificationAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notification.isRead").value(true))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].isRead").value(true));
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
        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/notifications/" + testNotification.getId() + "/read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notification.isRead").value(true));
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        Notification notification2 = new Notification(testUser1, testUser2, NotificationType.LIKE);
        notification2.setPost(testPost);
        notificationRepository.save(notification2);

        mockMvc.perform(put("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void markAllAsRead_NoUnreadNotifications() throws Exception {
        testNotification.setIsRead(true);
        notificationRepository.save(testNotification);

        mockMvc.perform(put("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        Notification notification2 = new Notification(testUser1, testUser2, NotificationType.LIKE);
        notification2.setPost(testPost);
        notificationRepository.save(notification2);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getUnreadCount_ZeroUnread() throws Exception {
        testNotification.setIsRead(true);
        notificationRepository.save(testNotification);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
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