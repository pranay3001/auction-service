package com.optimile.auction.repository;

import com.optimile.auction.model.entity.Notification;
import com.optimile.auction.model.enums.NotifCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, boolean isRead);
    List<Notification> findByUserIdAndCategoryAndIsReadOrderByCreatedAtDesc(UUID userId, NotifCategory category, boolean isRead);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllReadForUser(UUID userId);
}
