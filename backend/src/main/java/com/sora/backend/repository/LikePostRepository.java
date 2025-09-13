package com.sora.backend.repository;

import com.sora.backend.model.LikePost;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikePostRepository extends JpaRepository<LikePost, Long> {
    
    @Query("SELECT lp FROM LikePost lp WHERE lp.user.id = :userId AND lp.post.id = :postId")
    Optional<LikePost> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
    
    @Query("SELECT CASE WHEN COUNT(lp) > 0 THEN true ELSE false END FROM LikePost lp WHERE lp.user.id = :userId AND lp.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
    
    @Query("SELECT COUNT(lp) FROM LikePost lp WHERE lp.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
    
    @Query("SELECT lp FROM LikePost lp WHERE lp.post.id = :postId ORDER BY lp.createdAt DESC")
    Page<LikePost> findByPostId(@Param("postId") Long postId, Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM LikePost lp WHERE lp.user.id = :userId AND lp.post.id = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
    
    @Query("SELECT COUNT(lp) FROM LikePost lp WHERE lp.post.author.id = :userId")
    long countLikesReceivedByUserId(@Param("userId") Long userId);
}