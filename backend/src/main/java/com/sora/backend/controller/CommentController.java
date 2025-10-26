package com.sora.backend.controller;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Comment;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.PostRepository;
import com.sora.backend.service.CommentService;
import com.sora.backend.service.FollowService;
import com.sora.backend.service.LikeCommentService;
import com.sora.backend.service.PostService;
import com.sora.backend.service.UserAccountService;
import com.sora.backend.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@Tag(name = "Comments Management", description = "Post comments and replies")
public class CommentController {

    private final CommentService commentService;
    private final UserAccountService userAccountService;
    private final FollowService followService;
    private final LikeCommentService likeCommentService;

    @Autowired
    private PostRepository postRepository;

    public CommentController(CommentService commentService, UserAccountService userAccountService, FollowService followService, LikeCommentService likeCommentService) {
        this.commentService = commentService;
        this.userAccountService = userAccountService;
        this.followService = followService;
        this.likeCommentService = likeCommentService;
    }

    @GetMapping("/api/posts/{postId}/comments")
    @Operation(summary = "Get post comments", description = "Get paginated list of comments for a post")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<Page<CommentResponseDto>> getPostComments(@Parameter(description = "Post ID") @PathVariable Long postId, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").ascending());
        Page<Comment> comments = commentService.getPostComments(postId, pageable);
        Page<CommentResponseDto> responses = comments.map(c -> mapToCommentResponseDto(c, getCurrentUser(authentication)));
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/api/posts/{postId}/comments")
    @Operation(summary = "Create comment", description = "Create a new comment on a post")
    @ApiResponse(responseCode = "201", description = "Comment created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid comment content")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<CommentCreateResponseDto> createComment(@Parameter(description = "Post ID") @PathVariable Long postId, @Valid @RequestBody CommentCreateRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Comment comment = commentService.createComment(postId, currentUser, request.content());
        
        CommentResponseDto commentDto = mapToCommentResponseDto(comment, currentUser);
        CommentCreateResponseDto response = new CommentCreateResponseDto(
                MessageUtil.getMessage("comment.created.success"),
                commentDto
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/comments/{commentId}/reply")
    @Operation(summary = "Reply to comment", description = "Create a reply to an existing comment")
    @ApiResponse(responseCode = "201", description = "Reply created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid reply content")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<CommentCreateResponseDto> replyToComment(@Parameter(description = "Comment ID") @PathVariable Long commentId, @Valid @RequestBody CommentCreateRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Comment reply = commentService.replyToComment(commentId, currentUser, request.content());
        
        CommentResponseDto replyDto = mapToCommentResponseDto(reply, currentUser);
        CommentCreateResponseDto response = new CommentCreateResponseDto(
                MessageUtil.getMessage("comment.reply.created"),
                replyDto
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/comments/{commentId}")
    @Operation(summary = "Update comment", description = "Update comment content (author only)")
    @ApiResponse(responseCode = "200", description = "Comment updated successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to update this comment")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<CommentResponseDto> updateComment(@Parameter(description = "Comment ID") @PathVariable Long commentId, @Valid @RequestBody CommentCreateRequestDto request, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        Comment updatedComment = commentService.updateComment(commentId, currentUser, request.content());
        CommentResponseDto response = mapToCommentResponseDto(updatedComment, getCurrentUser(authentication));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/comments/{commentId}")
    @Operation(summary = "Delete comment", description = "Delete comment (author only)")
    @ApiResponse(responseCode = "200", description = "Comment deleted successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete this comment")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<MessageResponseDto> deleteComment(@Parameter(description = "Comment ID") @PathVariable Long commentId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        commentService.deleteComment(commentId, currentUser);
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("comment.deleted.success"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/comments/{commentId}/replies")
    @Operation(summary = "Get comment replies", description = "Get paginated list of replies to a comment")
    @ApiResponse(responseCode = "200", description = "Replies retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<Page<CommentResponseDto>> getCommentReplies(@Parameter(description = "Comment ID") @PathVariable Long commentId, @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").ascending());
        Page<Comment> replies = commentService.getCommentReplies(commentId, pageable);

        Page<CommentResponseDto> responses = replies.map(reply -> mapToCommentResponseDto(reply, getCurrentUser(authentication)));
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/api/comments/{commentId}/like")
    @Operation(summary = "Like comment", description = "Like a comment")
    @ApiResponse(responseCode = "200", description = "Comment liked successfully")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<MessageResponseDto> likeComment(@Parameter(description = "Comment ID") @PathVariable Long commentId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        likeCommentService.likeComment(currentUser, commentId);
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("comment.like.success"));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/comments/{commentId}/like")
    @Operation(summary = "Unlike comment", description = "Unlike a comment")
    @ApiResponse(responseCode = "200", description = "Comment unliked successfully")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<MessageResponseDto> unlikeComment(@Parameter(description = "Comment ID") @PathVariable Long commentId, Authentication authentication) {
        UserAccount currentUser = getCurrentUser(authentication);
        likeCommentService.unlikeComment(currentUser, commentId);
        MessageResponseDto response = new MessageResponseDto(MessageUtil.getMessage("comment.unlike.success"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/comments/{commentId}/likes/count")
    @Operation(summary = "Get comment likes count", description = "Get number of likes for a comment")
    @ApiResponse(responseCode = "200", description = "Likes count retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<Long> getCommentLikesCount(@Parameter(description = "Comment ID") @PathVariable Long commentId) {
        long count = likeCommentService.getCommentLikesCount(commentId);
        return ResponseEntity.ok(count);
    }

    private UserAccount getCurrentUser(Authentication authentication) {
        return userAccountService.findByEmail(authentication.getName()).orElseThrow();
    }

    private CommentResponseDto mapToCommentResponseDto(Comment comment, UserAccount currentUser) {
        UserSummaryDto authorDto = mapToUserSummaryDto(comment.getAuthor(), currentUser);

        List<Comment> replies = commentService.getCommentReplies(comment.getId());
        if (replies == null) {
            replies = Collections.emptyList();
        }

        int repliesCount = (int) commentService.getCommentRepliesCount(comment.getId());
        boolean isLiked = likeCommentService.isCommentLikedByUser(currentUser, comment.getId());

        return new CommentResponseDto(
                comment.getId(),
                authorDto,
                comment.getContent(),
                repliesCount,
                replies.stream().map(c -> mapToCommentResponseDto(c, currentUser)).toList(),
                isLiked,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private UserSummaryDto mapToUserSummaryDto(UserAccount user, UserAccount currentUser) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                postRepository.findDistinctCountriesByProfileOwnerId(user.getId()).size(),
                followService.isUserFollowing(currentUser, user.getId())
        );
    }
}