package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.LikePost;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.LikePostRepository;
import com.sora.backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class LikePostService {

    @Autowired
    private LikePostRepository likePostRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private NotificationService notificationService;


    public LikePost likePost(UserAccount user, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));

        Optional<LikePost> existingLikeOpt = likePostRepository.findByUserIdAndPostId(user.getId(), postId);

        if (existingLikeOpt.isPresent()) {
            return existingLikeOpt.get();
        }

        LikePost like = new LikePost();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());

        LikePost savedLike = likePostRepository.save(like);

        if (post.getAuthor() != null && !post.getAuthor().getId().equals(user.getId())) {
            notificationService.createLikeNotification(post.getAuthor(), user, post);
        }

        return savedLike;
    }

    public void unlikePost(UserAccount user, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        Optional<LikePost> likeOpt = likePostRepository.findByUserIdAndPostId(user.getId(), postId);

        if (likeOpt.isPresent()) {
            likePostRepository.deleteByUserIdAndPostId(user.getId(), postId);
        }
    }

    @Transactional(readOnly = true)
    public boolean isPostLikedByUser(UserAccount user, Long postId) {
        return likePostRepository.existsByUserIdAndPostId(user.getId(), postId);
    }

    @Transactional(readOnly = true)
    public long getPostLikesCount(Long postId) {
        // Verify post exists first
        postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        return likePostRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public Page<LikePost> getPostLikes(Long postId, Pageable pageable) {
        // Verify post exists first
        postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        return likePostRepository.findByPostId(postId, pageable);
    }
    
    public LikePost likePost(Long postId, UserAccount user) {
        return likePost(user, postId);
    }
    
    public void unlikePost(Long postId, UserAccount user) {
        unlikePost(user, postId);
    }
    
}