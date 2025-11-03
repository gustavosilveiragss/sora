package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Comment;
import com.sora.backend.model.LikeComment;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CommentRepository;
import com.sora.backend.repository.LikeCommentRepository;
import com.sora.backend.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class LikeCommentService {

    @Autowired
    private LikeCommentRepository likeCommentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    public LikeComment likeComment(UserAccount user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));

        Optional<LikeComment> existingLikeOpt = likeCommentRepository.findByUserIdAndCommentId(user.getId(), commentId);

        if (existingLikeOpt.isPresent()) {
            return existingLikeOpt.get();
        }

        LikeComment like = new LikeComment();
        like.setUser(user);
        like.setComment(comment);
        like.setCreatedAt(LocalDateTime.now());

        LikeComment savedLike = likeCommentRepository.save(like);

        return savedLike;
    }

    public void unlikeComment(UserAccount user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));

        Optional<LikeComment> likeOpt = likeCommentRepository.findByUserIdAndCommentId(user.getId(), commentId);

        if (likeOpt.isPresent()) {
            likeCommentRepository.deleteByUserIdAndCommentId(user.getId(), commentId);
        }
    }

    @Transactional(readOnly = true)
    public boolean isCommentLikedByUser(UserAccount user, Long commentId) {
        return likeCommentRepository.existsByUserIdAndCommentId(user.getId(), commentId);
    }

    @Transactional(readOnly = true)
    public long getCommentLikesCount(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));
        return likeCommentRepository.countByCommentId(commentId);
    }

    @Transactional(readOnly = true)
    public Page<LikeComment> getCommentLikes(Long commentId, Pageable pageable) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("comment.not.found")));
        return likeCommentRepository.findByCommentId(commentId, pageable);
    }

    public LikeComment likeComment(Long commentId, UserAccount user) {
        return likeComment(user, commentId);
    }

    public void unlikeComment(Long commentId, UserAccount user) {
        unlikeComment(user, commentId);
    }
}
