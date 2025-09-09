package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "reference_id")
    private String referenceId;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    public Notification() {}
    
    public Notification(UserAccount recipient, NotificationType type, String message, String referenceId) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.isRead = false;
    }
    
    public UserAccount getRecipient() {
        return recipient;
    }
    
    public void setRecipient(UserAccount recipient) {
        this.recipient = recipient;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}