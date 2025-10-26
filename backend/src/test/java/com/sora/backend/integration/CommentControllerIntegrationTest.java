package com.sora.backend.integration;

import com.sora.backend.dto.CommentCreateRequestDto;
import com.sora.backend.model.*;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    private Country testCountry;
    private Collection testCollection;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUpCommentTests() {
        createTestCountryAndCollection();
        createTestPost();
        createTestComment();
    }

    private void createTestCountryAndCollection() {
        testCountry = countryRepository.findByCode("BR").orElseThrow();
        testCollection = collectionRepository.findByCode("GENERAL").orElseThrow();
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
        testPost.setCaption("Test post for comments");
        testPost.setVisibilityType(PostVisibilityType.PERSONAL);
        testPost = postRepository.save(testPost);
    }

    private void createTestComment() {
        testComment = new Comment();
        testComment.setAuthor(testUser2);
        testComment.setPost(testPost);
        testComment.setContent("This is a test comment");
        testComment = commentRepository.save(testComment);
    }

    @Test
    void createComment_Success() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto(
            "Great post! Love this place 😍"
        );

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment.author.username").value("testuser2"))
                .andExpect(jsonPath("$.comment.content").value("Great post! Love this place 😍"))
                .andExpect(jsonPath("$.comment.createdAt").exists())
                .andExpect(jsonPath("$.comment.id").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsCount").value(2));
    }

    @Test
    void createComment_EmptyContent() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto("");

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_TooLongContent() throws Exception {
        String longContent = "a".repeat(1001);
        CommentCreateRequestDto request = new CommentCreateRequestDto(longContent);

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_PostNotFound() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto("Comment on non-existent post");

        mockMvc.perform(post("/api/posts/999999/comments")
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replyToComment_Success() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto(
            "Thanks for the comment! You should definitely visit 😊"
        );

        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/reply")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment.author.username").value("testuser1"))
                .andExpect(jsonPath("$.comment.content").value("Thanks for the comment! You should definitely visit 😊"))
                .andExpect(jsonPath("$.comment.createdAt").exists());
    }

    @Test
    void replyToComment_CommentNotFound() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto("Reply to non-existent comment");

        mockMvc.perform(post("/api/comments/999999/reply")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostComments_Success() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(testComment.getId()))
                .andExpect(jsonPath("$.content[0].author.username").value("testuser2"))
                .andExpect(jsonPath("$.content[0].content").value("This is a test comment"));
    }

    @Test
    void getPostComments_WithReplies() throws Exception {
        CommentCreateRequestDto replyRequest = new CommentCreateRequestDto("This is a reply");
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/reply")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(replyRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].repliesCount").value(1));
    }

    @Test
    void getPostComments_EmptyList() throws Exception {
        // Delete the test comment
        commentRepository.delete(testComment);

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getPostComments_PostNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/999999/comments")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateComment_Success() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto(
            "Updated: This is an amazing test comment! 🎉"
        );

        mockMvc.perform(put("/api/comments/" + testComment.getId())
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated: This is an amazing test comment! 🎉"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void updateComment_NotOwner() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto(
            "Trying to update someone else's comment"
        );

        mockMvc.perform(put("/api/comments/" + testComment.getId())
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateComment_CommentNotFound() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto("Update non-existent comment");

        mockMvc.perform(put("/api/comments/999999")
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComment_Success() throws Exception {
        mockMvc.perform(delete("/api/comments/" + testComment.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteComment_NotOwner() throws Exception {
        mockMvc.perform(delete("/api/comments/" + testComment.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComment_CommentNotFound() throws Exception {
        mockMvc.perform(delete("/api/comments/999999")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_Unauthorized() throws Exception {
        CommentCreateRequestDto request = new CommentCreateRequestDto("Unauthorized comment");

        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPostComments_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likeComment_Success() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/comments/" + testComment.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void likeComment_AlreadyLiked() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments/" + testComment.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void likeComment_CommentNotFound() throws Exception {
        mockMvc.perform(post("/api/comments/999999/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void likeComment_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unlikeComment_Success() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/comments/" + testComment.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void unlikeComment_NotLiked() throws Exception {
        mockMvc.perform(delete("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments/" + testComment.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void unlikeComment_CommentNotFound() throws Exception {
        mockMvc.perform(delete("/api/comments/999999/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unlikeComment_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/comments/" + testComment.getId() + "/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCommentLikesCount_Success() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments/" + testComment.getId() + "/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    void getCommentLikesCount_CommentNotFound() throws Exception {
        mockMvc.perform(get("/api/comments/999999/likes/count")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostComments_WithLikeStatus() throws Exception {
        mockMvc.perform(post("/api/comments/" + testComment.getId() + "/like")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isLikedByCurrentUser").value(true));

        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isLikedByCurrentUser").value(false));
    }
}