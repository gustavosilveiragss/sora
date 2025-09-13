package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Comment;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CommentRepository;
import com.sora.backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private NotificationService notificationService;


    public Comment createComment(UserAccount author, Long postId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));

        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        if (post.getAuthor() != null && !post.getAuthor().getId().equals(author.getId())) {
            notificationService.createPostCommentedNotification(post.getAuthor(), author, post);
        }

        return savedComment;
    }

    public Comment createReply(UserAccount author, Long parentCommentId, String content) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));

        if (parentComment.getParentComment() != null) {
            throw new ServiceException(MessageUtil.getMessage("comment.cannot.reply.to.reply"));
        }

        Comment reply = new Comment();
        reply.setAuthor(author);
        reply.setPost(parentComment.getPost());
        reply.setParentComment(parentComment);
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.now());

        Comment savedReply = commentRepository.save(reply);

        if (!parentComment.getAuthor().getId().equals(author.getId())) {
            notificationService.createPostCommentedNotification(parentComment.getAuthor(), author, parentComment.getPost());
        }

        if (parentComment.getPost().getAuthor() != null
                && !parentComment.getPost().getAuthor().getId().equals(author.getId())
                && !parentComment.getPost().getAuthor().getId().equals(parentComment.getAuthor().getId())) {
            notificationService.createPostCommentedNotification(parentComment.getPost().getAuthor(), author, parentComment.getPost());
        }

        return savedReply;
    }

    @Transactional(readOnly = true)
    public Page<Comment> getPostComments(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentReplies(Long commentId) {
        return commentRepository.findByParentCommentId(commentId);
    }

    @Transactional(readOnly = true)
    public long getPostCommentsCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public long getCommentRepliesCount(Long commentId) {
        return commentRepository.countByParentCommentId(commentId);
    }

    public Comment updateComment(Long commentId, UserAccount currentUser, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new ServiceException(MessageUtil.getMessage("comment.not.authorized"));
        }

        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, UserAccount currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new ServiceException(MessageUtil.getMessage("comment.not.authorized"));
        }

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        for (Comment reply : replies) {
            commentRepository.delete(reply);
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));
    }
    
    public Comment createComment(Long postId, UserAccount author, String content) {
        return createComment(author, postId, content);
    }
    
    public Comment replyToComment(Long commentId, UserAccount author, String content) {
        return createReply(author, commentId, content);
    }
    
    public Page<Comment> getCommentReplies(Long commentId, Pageable pageable) {
        List<Comment> replies = getCommentReplies(commentId);
        return new org.springframework.data.domain.PageImpl<>(replies, pageable, replies.size());
    }
    
}