package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.*;
import com.sora.backend.service.PostService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.service.LikePostService;
import com.sora.backend.service.CommentService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Post Management", description = "Post creation, management and media upload")
public class PostController {

    private final PostService postService;
    private final UserAccountService userAccountService;
    private final LikePostService likePostService;
    private final CommentService commentService;

    public PostController(PostService postService, UserAccountService userAccountService, LikePostService likePostService, CommentService commentService) {
        this.postService = postService;
        this.userAccountService = userAccountService;
        this.likePostService = likePostService;
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Create new post", description = "Create a new post with smart sharing options")
    @ApiResponse(responseCode = "201", description = "Post created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    public ResponseEntity<List<PostResponseDto>> createPost(@Valid @RequestBody PostCreateRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        List<Post> posts = postService.createPost(currentUser, request);
        List<PostResponseDto> responses = posts.stream().map(post -> mapToPostResponseDto(post, currentUser)).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/{postId}/media")
    @Operation(summary = "Upload post media", description = "Upload photos/videos for a post")
    @ApiResponse(responseCode = "201", description = "Media uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "403", description = "Not authorized to upload media for this post")
    public ResponseEntity<MediaUploadResponseDto> uploadMedia(@Parameter(description = "Post ID") @PathVariable Long postId, @RequestParam(value = "files", required = false) MultipartFile[] files, Authentication authentication) {
        if (files == null || files.length == 0) {
            throw new ServiceException(MessageUtil.getMessage("post.media.no_files"));
        }
        
        UserAccount currentUser = getCurrentUser(authentication);
        List<PostMedia> media = postService.uploadMedia(postId, files, currentUser);
        List<MediaDto> mediaDtos = media.stream().map(this::mapToMediaDto).toList();
        
        MediaUploadResponseDto response = new MediaUploadResponseDto(
            MessageUtil.getMessage("post.media.uploaded"),
            mediaDtos
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post details", description = "Get complete post details including media and stats")
    @ApiResponse(responseCode = "200", description = "Post retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponseDto> getPost(@Parameter(description = "Post ID") @PathVariable Long postId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Optional<Post> postOpt = postService.findById(postId);
        
        if (postOpt.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("post.not.found"));
        }
        
        Post post = postOpt.get();
        PostResponseDto response = mapToPostResponseDto(post, currentUser);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update post", description = "Update post caption and collection (with permission checks)")
    @ApiResponse(responseCode = "200", description = "Post updated successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to update this post")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponseDto> updatePost(@Parameter(description = "Post ID") @PathVariable Long postId, @RequestBody PostUpdateRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Post updatedPost = postService.updatePost(postId, request, currentUser);
        
        return ResponseEntity.ok(mapToPostResponseDto(updatedPost, currentUser));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post", description = "Delete post (independent control for shared posts)")
    @ApiResponse(responseCode = "200", description = "Post deleted successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete this post")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<MessageResponseDto> deletePost(@Parameter(description = "Post ID") @PathVariable Long postId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        postService.deletePost(postId, currentUser);
        
        MessageResponseDto response = new MessageResponseDto(
                MessageUtil.getMessage("post.deleted.success")
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared-group/{groupId}")
    @Operation(summary = "Get shared post group", description = "Get all posts in a shared group")
    @ApiResponse(responseCode = "200", description = "Shared posts retrieved successfully")
    public ResponseEntity<List<PostResponseDto>> getSharedPostGroup(@Parameter(description = "Shared post group ID") @PathVariable String groupId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        List<Post> posts = postService.getPostsBySharedGroup(groupId);
        List<PostResponseDto> responses = posts.stream().map(post -> mapToPostResponseDto(post, currentUser)).toList();
        return ResponseEntity.ok(responses);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private PostResponseDto mapToPostResponseDto(Post post, UserAccount currentUser) {
        return new PostResponseDto(
                post.getId(),
                mapToUserSummaryDto(post.getAuthor()),
                mapToUserSummaryDto(post.getProfileOwner()),
                mapToCountryDto(post.getCountry()),
                mapToCollectionDto(post.getCollection()),
                post.getCityName(),
                post.getCityLatitude(),
                post.getCityLongitude(),
                post.getCaption(),
                post.getMedia().stream().map(this::mapToMediaDto).toList(),
                (int) likePostService.getPostLikesCount(post.getId()),
                (int) commentService.getPostCommentsCount(post.getId()),
                likePostService.isPostLikedByUser(currentUser, post.getId()),
                post.getVisibilityType(),
                post.getSharedPostGroupId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
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

    private CountryDto mapToCountryDto(Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );
    }

    private CollectionDto mapToCollectionDto(Collection collection) {
        return new CollectionDto(
                collection.getId(),
                collection.getCode(),
                collection.getNameKey(),
                collection.getIconName(),
                collection.getSortOrder(),
                collection.getIsDefault()
        );
    }

    private MediaDto mapToMediaDto(PostMedia media) {
        String thumbnailUrl = media.getCloudinaryUrl() != null ? 
                media.getCloudinaryUrl().replace("/upload/", "/upload/c_fill,w_300,h_300/") : null;
        
        return new MediaDto(
                media.getId(),
                media.getFileName(),
                media.getCloudinaryPublicId(),
                media.getCloudinaryUrl(),
                thumbnailUrl,
                media.getMediaType(),
                media.getFileSize(),
                media.getWidth(),
                media.getHeight(),
                media.getSortOrder(),
                media.getUploadedAt()
        );
    }
}