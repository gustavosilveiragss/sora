package com.sora.backend.repository;

import com.sora.backend.model.Post;
import com.sora.backend.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    @Query("SELECT pm FROM PostMedia pm WHERE pm.post.id = :postId ORDER BY pm.sortOrder ASC")
    List<PostMedia> findByPostIdOrderBySortOrder(@Param("postId") Long postId);
    
    @Query("DELETE FROM PostMedia pm WHERE pm.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}