package com.sora.backend.repository;

import com.sora.backend.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    @Query("SELECT c FROM Country c WHERE c.code = :code")
    Optional<Country> findByCode(@Param("code") String code);
    
    @Query("SELECT c FROM Country c ORDER BY c.nameKey ASC")
    List<Country> findAllByOrderByNameKeyAsc();
}