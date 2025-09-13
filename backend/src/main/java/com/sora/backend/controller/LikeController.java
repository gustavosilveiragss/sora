package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.model.LikePost;
import com.sora.backend.model.UserAccount;
import com.sora.backend.service.LikePostService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Likes Management", description = "Post likes and engagement")
public class LikeController {

    private final LikePostService likePostService;
    private final UserAccountService userAccountService;

    public LikeController(LikePostService likePostService, UserAccountService userAccountService) {
        this.likePostService = likePostService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like a post", description = "Like a post (one like per user)")
    @ApiResponse(responseCode = "201", description = "Post liked successfully")
    @ApiResponse(responseCode = "400", description = "Post already liked or cannot like own post")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<LikeCreateResponseDto> likePost(@Parameter(description = "Post ID") @PathVariable Long postId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        LikePost like = likePostService.likePost(postId, currentUser);
        int likesCount = (int) likePostService.getPostLikesCount(postId);
        
        LikeResponseDto likeDto = mapToLikeResponseDto(like);
        LikeCreateResponseDto response = new LikeCreateResponseDto(
                MessageUtil.getMessage("post.liked.success"),
                likeDto,
                likesCount
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Unlike a post", description = "Remove like from a post")
    @ApiResponse(responseCode = "200", description = "Post unliked successfully")
    @ApiResponse(responseCode = "404", description = "Post not found or not liked")
    public ResponseEntity<MessageResponseDto> unlikePost(@Parameter(description = "Post ID") @PathVariable Long postId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        likePostService.unlikePost(postId, currentUser);
        
        MessageResponseDto response = new MessageResponseDto(
                MessageUtil.getMessage("post.unliked.success"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/likes")
    @Operation(summary = "Get post likes", description = "Get paginated list of users who liked the post")
    @ApiResponse(responseCode = "200", description = "Likes retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<Page<LikeResponseDto>> getPostLikes(@Parameter(description = "Post ID") @PathVariable Long postId, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        Page<LikePost> likes = likePostService.getPostLikes(postId, pageable);
        
        Page<LikeResponseDto> responses = likes.map(this::mapToLikeResponseDto);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{postId}/likes/count")
    @Operation(summary = "Get post likes count", description = "Get total number of likes for a post")
    @ApiResponse(responseCode = "200", description = "Likes count retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<LikesCountResponseDto> getPostLikesCount(@Parameter(description = "Post ID") @PathVariable Long postId) {
        int likesCount = (int) likePostService.getPostLikesCount(postId);
        LikesCountResponseDto response = new LikesCountResponseDto(likesCount);
        
        return ResponseEntity.ok(response);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private LikeResponseDto mapToLikeResponseDto(LikePost like) {
        UserSummaryDto userDto = mapToUserSummaryDto(like.getUser());
        
        return new LikeResponseDto(
                like.getId(),
                userDto,
                like.getCreatedAt()
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
}