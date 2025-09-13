package com.sora.backend.repository;

import com.sora.backend.model.Notification;
import com.sora.backend.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = :isRead ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndIsRead(@Param("recipientId") Long recipientId, @Param("isRead") Boolean isRead, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = :isRead")
    long countByRecipientIdAndIsRead(@Param("recipientId") Long recipientId, @Param("isRead") Boolean isRead);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = :isRead ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(@Param("recipientId") Long recipientId, @Param("isRead") Boolean isRead, Pageable pageable);
    
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.createdAt < :before")
    void deleteByRecipientIdAndCreatedAtBefore(@Param("recipientId") Long recipientId, @Param("before") LocalDateTime before);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = :isRead AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndIsReadAndType(@Param("recipientId") Long recipientId, @Param("isRead") Boolean isRead, @Param("type") com.sora.backend.model.NotificationType type, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndType(@Param("recipientId") Long recipientId, @Param("type") com.sora.backend.model.NotificationType type, Pageable pageable);
}