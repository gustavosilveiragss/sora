package com.sora.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_permission", uniqueConstraints = @UniqueConstraint(columnNames = {"grantor_id", "grantee_id", "country_id"}))
public class TravelPermission extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantor_id", nullable = false)
    @NotNull
    private UserAccount grantor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_id", nullable = false)
    @NotNull
    private UserAccount grantee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    @NotNull
    private Country country;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TravelPermissionStatus status = TravelPermissionStatus.PENDING;
    
    @Column(name = "invitation_message")
    private String invitationMessage;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    public TravelPermission() {}
    
    public TravelPermission(UserAccount grantor, UserAccount grantee, Country country, String message) {
        this.grantor = grantor;
        this.grantee = grantee;
        this.country = country;
        this.invitationMessage = message;
        this.status = TravelPermissionStatus.PENDING;
    }

    public UserAccount getGrantor() {
        return grantor;
    }
    
    public void setGrantor(UserAccount grantor) {
        this.grantor = grantor;
    }
    
    public UserAccount getGrantee() {
        return grantee;
    }
    
    public void setGrantee(UserAccount grantee) {
        this.grantee = grantee;
    }
    
    public Country getCountry() {
        return country;
    }
    
    public void setCountry(Country country) {
        this.country = country;
    }
    
    public TravelPermissionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TravelPermissionStatus status) {
        this.status = status;
    }
    
    public String getInvitationMessage() {
        return invitationMessage;
    }
    
    public void setInvitationMessage(String invitationMessage) {
        this.invitationMessage = invitationMessage;
    }
    
    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
    
    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
    
    public void accept() {
        this.status = TravelPermissionStatus.ACTIVE;
        this.respondedAt = LocalDateTime.now();
    }
    
    public void decline() {
        this.status = TravelPermissionStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }
    
    public void revoke() {
        this.status = TravelPermissionStatus.REVOKED;
        this.respondedAt = LocalDateTime.now();
    }
}