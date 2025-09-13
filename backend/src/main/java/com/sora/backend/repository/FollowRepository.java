package com.sora.backend.repository;

import com.sora.backend.model.Follow;
import com.sora.backend.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    Optional<Follow> findByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    Page<Follow> findByFollowerId(@Param("followerId") Long followerId, Pageable pageable);
    
    @Query("SELECT f FROM Follow f WHERE f.following.id = :followingId ORDER BY f.createdAt DESC")
    Page<Follow> findByFollowingId(@Param("followingId") Long followingId, Pageable pageable);
    
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :followingId")
    long countByFollowingId(@Param("followingId") Long followingId);
    
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :followerId")
    long countByFollowerId(@Param("followerId") Long followerId);
    
    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :followerId")
    List<UserAccount> findFollowingByFollowerId(@Param("followerId") Long followerId);
    
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    void deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId")
    List<Long> findFollowingUserIds(@Param("followerId") Long followerId);
    
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    long countFollowersByUserId(@Param("userId") Long userId);
}