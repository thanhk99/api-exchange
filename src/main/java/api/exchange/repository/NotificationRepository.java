package api.exchange.repository;

import api.exchange.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all notifications for a user, ordered by creation date (newest first)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Find unread notifications for a user
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    // Count unread notifications for a user
    Long countByUserIdAndIsReadFalse(String userId);

    // Find notification by ID and user ID (for security - user can only access
    // their own notifications)
    Optional<Notification> findByIdAndUserId(Long id, String userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);

    // Find notifications by order ID (useful for getting all notifications related
    // to a specific P2P order)
    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
