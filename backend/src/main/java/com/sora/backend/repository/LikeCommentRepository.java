package com.sora.backend.repository;

import com.sora.backend.model.LikeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeCommentRepository extends JpaRepository<LikeComment, Long> {

    @Query("SELECT lc FROM LikeComment lc WHERE lc.user.id = :userId AND lc.comment.id = :commentId")
    Optional<LikeComment> findByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Query("SELECT CASE WHEN COUNT(lc) > 0 THEN true ELSE false END FROM LikeComment lc WHERE lc.user.id = :userId AND lc.comment.id = :commentId")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Query("SELECT COUNT(lc) FROM LikeComment lc WHERE lc.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT lc FROM LikeComment lc WHERE lc.comment.id = :commentId ORDER BY lc.createdAt DESC")
    Page<LikeComment> findByCommentId(@Param("commentId") Long commentId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM LikeComment lc WHERE lc.user.id = :userId AND lc.comment.id = :commentId")
    void deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Query("SELECT COUNT(lc) FROM LikeComment lc WHERE lc.comment.author.id = :userId")
    long countLikesReceivedByUserId(@Param("userId") Long userId);
}
