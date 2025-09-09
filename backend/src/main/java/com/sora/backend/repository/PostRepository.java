package com.sora.backend.repository;

import com.sora.backend.model.Collection;
import com.sora.backend.model.Country;
import com.sora.backend.model.Post;
import com.sora.backend.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.sora.backend.dto.LastActiveCountryDto;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    @Query("SELECT p FROM Post p WHERE p.profileOwner.id = :profileOwnerId ORDER BY p.createdAt DESC")
    Page<Post> findByProfileOwnerIdOrderByCreatedAtDesc(@Param("profileOwnerId") Long profileOwnerId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.profileOwner.id = :profileOwnerId AND p.country.id = :countryId ORDER BY p.createdAt DESC")
    Page<Post> findByProfileOwnerIdAndCountryIdOrderByCreatedAtDesc(@Param("profileOwnerId") Long profileOwnerId, @Param("countryId") Long countryId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.profileOwner.id = :profileOwnerId AND p.country.id = :countryId AND p.collection.id = :collectionId ORDER BY p.createdAt DESC")
    Page<Post> findByProfileOwnerIdAndCountryIdAndCollectionIdOrderByCreatedAtDesc(@Param("profileOwnerId") Long profileOwnerId, @Param("countryId") Long countryId, @Param("collectionId") Long collectionId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.sharedPostGroupId = :sharedPostGroupId")
    List<Post> findBySharedPostGroupId(@Param("sharedPostGroupId") String sharedPostGroupId);
    
    @Query("SELECT p FROM Post p WHERE p.profileOwner.id IN :userIds ORDER BY p.createdAt DESC")
    Page<Post> findByProfileOwnerIdInOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.country.id = :countryId AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Post> findByCountryIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("countryId") Long countryId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId")
    long countByAuthorId(@Param("authorId") Long authorId);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.profileOwner.id = :profileOwnerId AND p.country.id = :countryId")
    long countByProfileOwnerIdAndCountryId(@Param("profileOwnerId") Long profileOwnerId, @Param("countryId") Long countryId);
    
    @Query("SELECT COUNT(DISTINCT p.country) FROM Post p WHERE p.profileOwner.id = :userId")
    long countDistinctCountriesByProfileOwnerId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(DISTINCT p.cityName) FROM Post p WHERE p.profileOwner.id = :userId")
    long countDistinctCitiesByProfileOwnerId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT p.country FROM Post p WHERE p.profileOwner.id = :userId ORDER BY MAX(p.createdAt) DESC")
    List<Country> findDistinctCountriesByProfileOwnerId(@Param("userId") Long userId);
    
    @Query("SELECT MIN(p.createdAt) FROM Post p WHERE p.profileOwner.id = :userId AND p.country.id = :countryId")
    LocalDateTime findFirstPostDateInCountryByIds(@Param("userId") Long userId, @Param("countryId") Long countryId);
    
    @Query("SELECT MAX(p.createdAt) FROM Post p WHERE p.profileOwner.id = :userId AND p.country.id = :countryId")
    LocalDateTime findLastPostDateInCountryByIds(@Param("userId") Long userId, @Param("countryId") Long countryId);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Post p WHERE p.profileOwner.id = :profileOwnerId AND p.country.id = :countryId")
    boolean existsByProfileOwnerIdAndCountryId(@Param("profileOwnerId") Long profileOwnerId, @Param("countryId") Long countryId);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.profileOwner.id = :userId")
    long countByProfileOwnerId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT c.code FROM Post p JOIN p.country c WHERE p.profileOwner.id IN (:userId1, :userId2) GROUP BY c.code HAVING COUNT(DISTINCT p.profileOwner.id) = 2")
    List<String> findCommonCountriesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT new com.sora.backend.dto.LastActiveCountryDto(c.code, c.nameKey, MAX(p.createdAt), CAST(COUNT(p) AS int)) FROM Post p JOIN p.country c WHERE p.profileOwner.id = :userId GROUP BY c.code, c.nameKey ORDER BY MAX(p.createdAt) DESC")
    List<LastActiveCountryDto> findLastActiveCountriesByUserId(@Param("userId") Long userId);
}