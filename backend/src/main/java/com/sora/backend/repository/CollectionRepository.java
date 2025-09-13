package com.sora.backend.repository;

import com.sora.backend.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    @Query("SELECT c FROM Collection c WHERE c.code = :code")
    Optional<Collection> findByCode(@Param("code") String code);
    
    @Query("SELECT c FROM Collection c ORDER BY c.sortOrder ASC")
    List<Collection> findAllByOrderBySortOrderAsc();
    
    @Query("SELECT c FROM Collection c WHERE c.isDefault = true")
    Optional<Collection> findByIsDefaultTrue();
}