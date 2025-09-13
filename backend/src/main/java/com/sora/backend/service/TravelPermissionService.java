package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.exception.UnauthorizedException;
import com.sora.backend.model.Country;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.repository.PostRepository;
import com.sora.backend.repository.TravelPermissionRepository;
import com.sora.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class TravelPermissionService {

    @Autowired
    private TravelPermissionRepository travelPermissionRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private NotificationService notificationService;


    public TravelPermission createPermissionInvitation(UserAccount grantor, String granteeUsername, String countryCode, String invitationMessage) {
        Country country = countryRepository.findByCode(countryCode)
            .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));

        UserAccount grantee = userAccountRepository.findByUsername(granteeUsername)
            .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));

        if (grantor.getId().equals(grantee.getId())) {
            throw new ServiceException(MessageUtil.getMessage("travel.permission.cannot.grant.self"));
        }

        if (!postRepository.existsByProfileOwnerIdAndCountryId(grantor.getId(), country.getId())) {
            throw new ServiceException(MessageUtil.getMessage("travel.permission.must.have.visited"));
        }

        Optional<TravelPermission> existingOpt = travelPermissionRepository.findByGrantorIdAndGranteeIdAndCountryId(grantor.getId(), grantee.getId(), country.getId());

        if (existingOpt.isPresent()) {
            TravelPermission existing = existingOpt.get();
            if (existing.getStatus() == TravelPermissionStatus.PENDING || existing.getStatus() == TravelPermissionStatus.ACTIVE) {
                throw new ServiceException(MessageUtil.getMessage("travel.permission.already.exists"));
            }
            existing.setStatus(TravelPermissionStatus.PENDING);
            existing.setInvitationMessage(invitationMessage);
            existing.setCreatedAt(LocalDateTime.now());
            existing.setRespondedAt(null);
            
            TravelPermission savedPermission = travelPermissionRepository.save(existing);
            notificationService.createTravelPermissionInvitation(grantee, grantor, country, savedPermission.getId());
            return savedPermission;
        } else {
            TravelPermission permission = new TravelPermission();
            permission.setGrantor(grantor);
            permission.setGrantee(grantee);
            permission.setCountry(country);
            permission.setStatus(TravelPermissionStatus.PENDING);
            permission.setInvitationMessage(invitationMessage);
            permission.setCreatedAt(LocalDateTime.now());
            
            TravelPermission savedPermission = travelPermissionRepository.save(permission);
            notificationService.createTravelPermissionInvitation(grantee, grantor, country, savedPermission.getId());
            return savedPermission;
        }
    }

    public TravelPermission acceptPermission(Long permissionId, UserAccount grantee) {
        TravelPermission permission = travelPermissionRepository.findById(permissionId)
            .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("travel.permission.not.found")));

        if (!permission.getGrantee().getId().equals(grantee.getId())) {
            throw new UnauthorizedException(MessageUtil.getMessage("travel.permission.not.authorized"));
        }

        if (permission.getStatus() != TravelPermissionStatus.PENDING) {
            throw new ServiceException(MessageUtil.getMessage("travel.permission.not.pending"));
        }

        permission.setStatus(TravelPermissionStatus.ACTIVE);
        permission.setRespondedAt(LocalDateTime.now());
        
        TravelPermission savedPermission = travelPermissionRepository.save(permission);
        notificationService.createTravelPermissionAccepted(permission.getGrantor(), grantee, permission.getCountry());
        return savedPermission;
    }

    public TravelPermission declinePermission(Long permissionId, UserAccount grantee) {
        TravelPermission permission = travelPermissionRepository.findById(permissionId)
            .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("travel.permission.not.found")));

        if (!permission.getGrantee().getId().equals(grantee.getId())) {
            throw new UnauthorizedException(MessageUtil.getMessage("travel.permission.not.authorized"));
        }

        if (permission.getStatus() != TravelPermissionStatus.PENDING) {
            throw new ServiceException(MessageUtil.getMessage("travel.permission.not.pending"));
        }

        permission.setStatus(TravelPermissionStatus.DECLINED);
        permission.setRespondedAt(LocalDateTime.now());
        
        TravelPermission savedPermission = travelPermissionRepository.save(permission);
        notificationService.createTravelPermissionDeclined(permission.getGrantor(), grantee, permission.getCountry());
        return savedPermission;
    }

    public TravelPermission revokePermission(Long permissionId, UserAccount grantor) {
        TravelPermission permission = travelPermissionRepository.findById(permissionId)
            .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("travel.permission.not.found")));

        if (!permission.getGrantor().getId().equals(grantor.getId())) {
            throw new UnauthorizedException(MessageUtil.getMessage("travel.permission.not.authorized"));
        }

        if (permission.getStatus() != TravelPermissionStatus.ACTIVE) {
            throw new ServiceException(MessageUtil.getMessage("travel.permission.not.active"));
        }

        permission.setStatus(TravelPermissionStatus.REVOKED);
        permission.setRespondedAt(LocalDateTime.now());
        
        TravelPermission savedPermission = travelPermissionRepository.save(permission);
        notificationService.createTravelPermissionRevoked(permission.getGrantee(), grantor, permission.getCountry());
        return savedPermission;
    }

    @Transactional(readOnly = true)
    public List<TravelPermission> getGrantedPermissions(UserAccount grantor, TravelPermissionStatus status) {
        if (status != null)
            return travelPermissionRepository.findByGrantorIdAndStatus(grantor.getId(), status);
        return travelPermissionRepository.findByGrantorIdAndStatus(grantor.getId(), TravelPermissionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<TravelPermission> getReceivedPermissions(UserAccount grantee, TravelPermissionStatus status) {
        if (status != null)
            return travelPermissionRepository.findByGranteeIdAndStatus(grantee.getId(), status);
        return travelPermissionRepository.findByGranteeIdAndStatus(grantee.getId(), TravelPermissionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public boolean hasActivePermission(UserAccount grantee, String countryCode) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        return travelPermissionRepository.existsByGranteeIdAndCountryIdAndStatus(grantee.getId(), country.getId(), TravelPermissionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<TravelPermission> getActiveCollaborators(UserAccount grantor, String countryCode) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        return travelPermissionRepository.findByGrantorIdAndCountryIdAndStatus(grantor.getId(), country.getId(), TravelPermissionStatus.ACTIVE);
    }

    public TravelPermission grantPermission(UserAccount grantor, String granteeUsername, String countryCode, String invitationMessage) {
        return createPermissionInvitation(grantor, granteeUsername, countryCode, invitationMessage);
    }

    public org.springframework.data.domain.Page<TravelPermission> getGrantedPermissions(UserAccount grantor, String status, org.springframework.data.domain.Pageable pageable) {
        TravelPermissionStatus permissionStatus = null;
        if (status != null) {
            try {
                permissionStatus = TravelPermissionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return org.springframework.data.domain.Page.empty(pageable);
            }
        }
        
        if (permissionStatus != null) {
            return travelPermissionRepository.findByGrantorIdAndStatus(grantor.getId(), permissionStatus, pageable);
        } else {
            return travelPermissionRepository.findByGrantorId(grantor.getId(), pageable);
        }
    }

    public org.springframework.data.domain.Page<TravelPermission> getReceivedPermissions(UserAccount grantee, String status, org.springframework.data.domain.Pageable pageable) {
        TravelPermissionStatus permissionStatus = null;
        if (status != null) {
            try {
                permissionStatus = TravelPermissionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return org.springframework.data.domain.Page.empty(pageable);
            }
        }
        
        if (permissionStatus != null) {
            return travelPermissionRepository.findByGranteeIdAndStatus(grantee.getId(), permissionStatus, pageable);
        } else {
            return travelPermissionRepository.findByGranteeId(grantee.getId(), pageable);
        }
    }

    public int getCollaborativePostsCount(TravelPermission permission) {
        return (int) postRepository.countByAuthorIdAndCountryIdAndProfileOwnerIdAndCreatedAtAfter(
            permission.getGrantee().getId(),
            permission.getCountry().getId(),
            permission.getGrantor().getId(),
            permission.getCreatedAt()
        );
    }
}