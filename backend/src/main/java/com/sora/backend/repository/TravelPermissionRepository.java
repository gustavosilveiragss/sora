package com.sora.backend.repository;

import com.sora.backend.model.Country;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelPermissionRepository extends JpaRepository<TravelPermission, Long> {
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantor.id = :grantorId AND tp.grantee.id = :granteeId AND tp.country.id = :countryId")
    Optional<TravelPermission> findByGrantorIdAndGranteeIdAndCountryId(@Param("grantorId") Long grantorId, @Param("granteeId") Long granteeId, @Param("countryId") Long countryId);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantee.id = :granteeId AND tp.status = :status")
    List<TravelPermission> findByGranteeIdAndStatus(@Param("granteeId") Long granteeId, @Param("status") TravelPermissionStatus status);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantor.id = :grantorId AND tp.status = :status")
    List<TravelPermission> findByGrantorIdAndStatus(@Param("grantorId") Long grantorId, @Param("status") TravelPermissionStatus status);
    
    @Query("SELECT CASE WHEN COUNT(tp) > 0 THEN true ELSE false END FROM TravelPermission tp WHERE tp.grantee.id = :granteeId AND tp.country.id = :countryId AND tp.status = :status")
    boolean existsByGranteeIdAndCountryIdAndStatus(@Param("granteeId") Long granteeId, @Param("countryId") Long countryId, @Param("status") TravelPermissionStatus status);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantor.id = :grantorId AND tp.country.id = :countryId AND tp.status = :status")
    List<TravelPermission> findByGrantorIdAndCountryIdAndStatus(@Param("grantorId") Long grantorId, @Param("countryId") Long countryId, @Param("status") TravelPermissionStatus status);

    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantor.id = :grantorId AND tp.status = :status")
    Page<TravelPermission> findByGrantorIdAndStatus(@Param("grantorId") Long grantorId, @Param("status") TravelPermissionStatus status, Pageable pageable);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantee.id = :granteeId AND tp.status = :status")
    Page<TravelPermission> findByGranteeIdAndStatus(@Param("granteeId") Long granteeId, @Param("status") TravelPermissionStatus status, Pageable pageable);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantor.id = :grantorId")
    Page<TravelPermission> findByGrantorId(@Param("grantorId") Long grantorId, Pageable pageable);
    
    @Query("SELECT tp FROM TravelPermission tp WHERE tp.grantee.id = :granteeId")
    Page<TravelPermission> findByGranteeId(@Param("granteeId") Long granteeId, Pageable pageable);
}