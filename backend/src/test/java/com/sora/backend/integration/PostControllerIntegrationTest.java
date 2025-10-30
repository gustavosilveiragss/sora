package com.sora.backend.integration;

import com.sora.backend.config.TestCloudinaryConfig;
import com.sora.backend.dto.PostCreateRequestDto;
import com.sora.backend.dto.PostUpdateRequestDto;
import com.sora.backend.model.Collection;
import com.sora.backend.model.Country;
import com.sora.backend.model.Follow;
import com.sora.backend.model.LikePost;
import com.sora.backend.model.Post;
import com.sora.backend.model.PostSharingOption;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.TravelPermissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCloudinaryConfig.class)
class PostControllerIntegrationTest extends BaseIntegrationTest {

    private Country testCountry;
    private Collection testCollection;
    private Post testPost;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpPostTests() {
        setupMinimalTestData();
    }

    private void setupMinimalTestData() {
        testCountry = countryRepository.findByCode("BR").orElseGet(() -> {
            Country country = new Country();
            country.setCode("BR");
            country.setNameKey("country.brazil");
            country.setLatitude(-14.235);
            country.setLongitude(-51.9253);
            country.setTimezone("America/Sao_Paulo");
            return countryRepository.save(country);
        });

        testCollection = collectionRepository.findByCode("GENERAL").orElseGet(() -> {
            Collection collection = new Collection();
            collection.setCode("GENERAL");
            collection.setNameKey("collection.general");
            collection.setIconName("general");
            collection.setSortOrder(1);
            collection.setIsDefault(true);
            return collectionRepository.save(collection);
        });

        setupCollaboration();

        createTestPost();
    }
    
    private void setupCollaboration() {
        TravelPermission permission = new TravelPermission();
        permission.setGrantor(testUser2);
        permission.setGrantee(testUser1);
        permission.setCountry(testCountry);
        permission.setStatus(TravelPermissionStatus.ACTIVE);
        permission.setCreatedAt(java.time.LocalDateTime.now());
        travelPermissionRepository.save(permission);
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
        testPost.setCaption("Test post caption");
        testPost.setVisibilityType(PostVisibilityType.PERSONAL);
        testPost = postRepository.save(testPost);
    }

    @Test
    void createPost_Success() throws Exception {
        PostCreateRequestDto request = new PostCreateRequestDto(
            testCountry.getCode(),
            testCollection.getCode(),
            "Rio de Janeiro",
            -22.9068,
            -43.1729,
            "Amazing view from Sugarloaf Mountain! 🏔️",
            PostSharingOption.PERSONAL_ONLY,
            null,
            "PERSONAL_ONLY"
        );

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].author.username").value("testuser1"))
                .andExpect(jsonPath("$[0].country.code").value("BR"))
                .andExpect(jsonPath("$[0].collection.code").value("GENERAL"))
                .andExpect(jsonPath("$[0].cityName").value("Rio de Janeiro"))
                .andExpect(jsonPath("$[0].caption").value("Amazing view from Sugarloaf Mountain! 🏔️"))
                .andExpect(jsonPath("$[0].visibilityType").value("PERSONAL"));
    }

    @Test
    void createPost_InvalidCountry() throws Exception {
        PostCreateRequestDto request = new PostCreateRequestDto(
            "INVALID_COUNTRY",
            testCollection.getCode(),
            "São Paulo",
            -23.5558,
            -46.6396,
            "Test caption",
            PostSharingOption.PERSONAL_ONLY,
            null,
            "PERSONAL_ONLY"
        );

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_InvalidCollection() throws Exception {
        PostCreateRequestDto request = new PostCreateRequestDto(
            testCountry.getCode(),
            "INVALID_COLLECTION",
            "São Paulo",
            -23.5558,
            -46.6396,
            "Test caption",
            PostSharingOption.PERSONAL_ONLY,
            null,
            "PERSONAL_ONLY"
        );

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_CollaborateWithUser() throws Exception {
        PostCreateRequestDto request = new PostCreateRequestDto(
            testCountry.getCode(),
            testCollection.getCode(),
            "São Paulo",
            -23.5558,
            -46.6396,
            "Collaborative post caption",
            PostSharingOption.BOTH_PROFILES,
            testUser2.getId(),
            "BOTH_PROFILES"
        );

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].visibilityType").value("SHARED"))
                .andExpect(jsonPath("$[0].sharedPostGroupId").exists());
    }

    @Test
    void getPostDetails_Success() throws Exception {
        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPost.getId()))
                .andExpect(jsonPath("$.author.username").value("testuser1"))
                .andExpect(jsonPath("$.caption").value("Test post caption"))
                .andExpect(jsonPath("$.country.code").value("BR"))
                .andExpect(jsonPath("$.collection.code").value("GENERAL"))
                .andExpect(jsonPath("$.cityName").value("São Paulo"))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0))
                .andExpect(jsonPath("$.isLikedByCurrentUser").value(false));
    }

    @Test
    void getPostDetails_PostNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/999999")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePost_Success() throws Exception {
        PostUpdateRequestDto request = new PostUpdateRequestDto(
            "Updated test post caption with new content! 🎉",
            testCollection.getCode()
        );

        mockMvc.perform(put("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Updated test post caption with new content! 🎉"));
    }

    @Test
    void updatePost_NotOwner() throws Exception {
        PostUpdateRequestDto request = new PostUpdateRequestDto(
            "Trying to update someone else's post",
            testCollection.getCode()
        );

        mockMvc.perform(put("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePost_PostNotFound() throws Exception {
        PostUpdateRequestDto request = new PostUpdateRequestDto(
            "Updated caption",
            testCollection.getCode()
        );

        mockMvc.perform(put("/api/posts/999999")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePost_Success() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePost_NotOwner() throws Exception {
        mockMvc.perform(delete("/api/posts/" + testPost.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_PostNotFound() throws Exception {
        mockMvc.perform(delete("/api/posts/999999")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPostMedia_Success() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get("src/test/java/com/sora/backend/integration/paises-del-mundo.jpg"));
        
        MockMultipartFile file1 = new MockMultipartFile(
            "files",
            "paises-del-mundo.jpg",
            "image/jpeg",
            imageBytes
        );

        MockMultipartFile file2 = new MockMultipartFile(
            "files",
            "paises-del-mundo-copy.jpg",
            "image/jpeg",
            imageBytes
        );

        mockMvc.perform(multipart("/api/posts/" + testPost.getId() + "/media")
                .file(file1)
                .file(file2)
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.media").isArray())
                .andExpect(jsonPath("$.media", hasSize(2)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void uploadPostMedia_NoFiles() throws Exception {
        mockMvc.perform(multipart("/api/posts/" + testPost.getId() + "/media")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPostMedia_NotOwner() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "image.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/posts/" + testPost.getId() + "/media")
                .file(file)
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadPostMedia_PostNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "image.jpg", 
            "image/jpeg",
            "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/posts/999999/media")
                .file(file)
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSharedPostGroup_Success() throws Exception {
        PostCreateRequestDto request = new PostCreateRequestDto(
            testCountry.getCode(),
            testCollection.getCode(),
            "São Paulo",
            -23.5558,
            -46.6396,
            "Shared post caption",
            PostSharingOption.BOTH_PROFILES,
            testUser2.getId(),
            "BOTH_PROFILES"
        );

        String createResponse = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.List<java.util.Map<String, Object>> posts = objectMapper.readValue(createResponse, new TypeReference<>() {});
        String sharedPostGroupId = (String) posts.getFirst().get("sharedPostGroupId");

        mockMvc.perform(get("/api/posts/shared-group/" + sharedPostGroupId)
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSharedPostGroup_NotFound() throws Exception {
        mockMvc.perform(get("/api/posts/shared-group/non-existent-group-id")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFeed_WithFollowedUsersAndOwnPosts() throws Exception {
        Follow follow = new Follow();
        follow.setFollower(testUser1);
        follow.setFollowing(testUser2);
        follow.setCreatedAt(java.time.LocalDateTime.now());
        followRepository.save(follow);

        Post user2Post = new Post();
        user2Post.setAuthor(testUser2);
        user2Post.setProfileOwner(testUser2);
        user2Post.setCountry(testCountry);
        user2Post.setCollection(testCollection);
        user2Post.setCityName("Rio de Janeiro");
        user2Post.setCityLatitude(-22.9068);
        user2Post.setCityLongitude(-43.1729);
        user2Post.setCaption("User 2 post");
        user2Post.setVisibilityType(PostVisibilityType.PERSONAL);
        user2Post.setCreatedAt(java.time.LocalDateTime.now());
        postRepository.save(user2Post);

        mockMvc.perform(get("/api/posts/feed")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getFeed_OnlyOwnPostsWhenNotFollowingAnyone() throws Exception {
        mockMvc.perform(get("/api/posts/feed")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].author.username").value("testuser1"));
    }

    @Test
    void getFeed_WithPagination() throws Exception {
        for (int i = 0; i < 25; i++) {
            Post post = new Post();
            post.setAuthor(testUser1);
            post.setProfileOwner(testUser1);
            post.setCountry(testCountry);
            post.setCollection(testCollection);
            post.setCityName("City " + i);
            post.setCityLatitude(-23.5558);
            post.setCityLongitude(-46.6396);
            post.setCaption("Post number " + i);
            post.setVisibilityType(PostVisibilityType.PERSONAL);
            post.setCreatedAt(java.time.LocalDateTime.now().minusMinutes(i));
            postRepository.save(post);
        }

        mockMvc.perform(get("/api/posts/feed")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(26))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        mockMvc.perform(get("/api/posts/feed")
                .param("page", "2")
                .param("size", "10")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getFeed_OrderedByCreatedAtDesc() throws Exception {
        postRepository.deleteAll();

        Follow follow = new Follow();
        follow.setFollower(testUser1);
        follow.setFollowing(testUser2);
        follow.setCreatedAt(java.time.LocalDateTime.now());
        followRepository.save(follow);

        Post oldPost = new Post();
        oldPost.setAuthor(testUser2);
        oldPost.setProfileOwner(testUser2);
        oldPost.setCountry(testCountry);
        oldPost.setCollection(testCollection);
        oldPost.setCityName("Old City");
        oldPost.setCityLatitude(-22.9068);
        oldPost.setCityLongitude(-43.1729);
        oldPost.setCaption("Old post");
        oldPost.setVisibilityType(PostVisibilityType.PERSONAL);
        oldPost.setCreatedAt(java.time.LocalDateTime.now().minusDays(2));
        postRepository.save(oldPost);

        Post middlePost = new Post();
        middlePost.setAuthor(testUser1);
        middlePost.setProfileOwner(testUser1);
        middlePost.setCountry(testCountry);
        middlePost.setCollection(testCollection);
        middlePost.setCityName("Middle City");
        middlePost.setCityLatitude(-23.5558);
        middlePost.setCityLongitude(-46.6396);
        middlePost.setCaption("Middle post");
        middlePost.setVisibilityType(PostVisibilityType.PERSONAL);
        middlePost.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));
        postRepository.save(middlePost);

        Post newPost = new Post();
        newPost.setAuthor(testUser2);
        newPost.setProfileOwner(testUser2);
        newPost.setCountry(testCountry);
        newPost.setCollection(testCollection);
        newPost.setCityName("New City");
        newPost.setCityLatitude(-22.9068);
        newPost.setCityLongitude(-43.1729);
        newPost.setCaption("New post");
        newPost.setVisibilityType(PostVisibilityType.PERSONAL);
        newPost.setCreatedAt(java.time.LocalDateTime.now());
        postRepository.save(newPost);

        mockMvc.perform(get("/api/posts/feed")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].caption").value("New post"))
                .andExpect(jsonPath("$.content[1].caption").value("Middle post"))
                .andExpect(jsonPath("$.content[2].caption").value("Old post"));
    }

    @Test
    void getFeed_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/feed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getExplorePosts_WithDefaultTimeframe() throws Exception {
        mockMvc.perform(get("/api/posts/explore")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getExplorePosts_WithWeekTimeframe() throws Exception {
        mockMvc.perform(get("/api/posts/explore")
                        .param("timeframe", "week")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getExplorePosts_WithMonthTimeframe() throws Exception {
        mockMvc.perform(get("/api/posts/explore")
                        .param("timeframe", "month")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getExplorePosts_WithAllTimeframe() throws Exception {
        mockMvc.perform(get("/api/posts/explore")
                        .param("timeframe", "all")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getExplorePosts_OrderedByEngagement() throws Exception {
        Post recentPost = new Post();
        recentPost.setAuthor(testUser2);
        recentPost.setProfileOwner(testUser2);
        recentPost.setCountry(testCountry);
        recentPost.setCollection(testCollection);
        recentPost.setCityName("Recent City");
        recentPost.setCityLatitude(-23.5505);
        recentPost.setCityLongitude(-46.6333);
        recentPost.setCaption("Recent post with less engagement");
        recentPost.setCreatedAt(LocalDateTime.now().minusHours(1));
        recentPost.setUpdatedAt(LocalDateTime.now().minusHours(1));
        postRepository.save(recentPost);

        Post olderPost = new Post();
        olderPost.setAuthor(testUser2);
        olderPost.setProfileOwner(testUser2);
        olderPost.setCountry(testCountry);
        olderPost.setCollection(testCollection);
        olderPost.setCityName("Older City");
        olderPost.setCityLatitude(-23.5505);
        olderPost.setCityLongitude(-46.6333);
        olderPost.setCaption("Older post");
        olderPost.setCreatedAt(LocalDateTime.now().minusDays(3));
        olderPost.setUpdatedAt(LocalDateTime.now().minusDays(3));
        postRepository.save(olderPost);

        mockMvc.perform(get("/api/posts/explore")
                        .param("timeframe", "week")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getExplorePosts_WithPagination() throws Exception {
        for (int i = 0; i < 25; i++) {
            Post post = new Post();
            post.setAuthor(testUser2);
            post.setProfileOwner(testUser2);
            post.setCountry(testCountry);
            post.setCollection(testCollection);
            post.setCityName("Test City " + i);
            post.setCityLatitude(-23.5505);
            post.setCityLongitude(-46.6333);
            post.setCaption("Test post " + i);
            post.setCreatedAt(LocalDateTime.now().minusHours(i));
            post.setUpdatedAt(LocalDateTime.now().minusHours(i));
            postRepository.save(post);
        }

        mockMvc.perform(get("/api/posts/explore")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(26))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getExplorePosts_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/explore"))
                .andExpect(status().isUnauthorized());
    }
}