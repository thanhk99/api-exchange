package api.exchange.services;

import api.exchange.models.Notification;
import api.exchange.models.Notification.NotificationType;
import api.exchange.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SseNotificationService sseNotificationService;

    /**
     * Create and save a notification, then send via SSE if user is connected
     */
    @Transactional
    public Notification createNotification(String userId, String title, String content,
            NotificationType type, Long orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationTitle(title);
        notification.setNotificationContent(content);
        notification.setNotificationType(type);
        notification.setOrderId(orderId);
        notification.setRead(false);

        // Save to database
        Notification saved = notificationRepository.save(notification);

        // Send via SSE if user is connected
        if (sseNotificationService.isUserConnected(userId)) {
            sseNotificationService.sendNotification(userId, saved);
        }

        return saved;
    }

    /**
     * Get user's notifications with pagination
     */
    public ResponseEntity<?> getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", Map.of(
                        "notifications", notifications.getContent(),
                        "currentPage", notifications.getNumber(),
                        "totalPages", notifications.getTotalPages(),
                        "totalItems", notifications.getTotalElements())));
    }

    /**
     * Get unread notifications for a user
     */
    public ResponseEntity<?> getUnreadNotifications(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", unreadNotifications));
    }

    /**
     * Get count of unread notifications
     */
    public ResponseEntity<?> getUnreadCount(String userId) {
        Long count = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", Map.of("count", count)));
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public ResponseEntity<?> markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElse(null);

        if (notification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Notification not found"));
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            notificationRepository.save(notification);
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Notification marked as read",
                "data", notification));
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public ResponseEntity<?> markAllAsRead(String userId) {
        LocalDateTime readAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId, readAt);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "All notifications marked as read",
                "data", Map.of("updatedCount", updatedCount)));
    }

    /**
     * Delete a notification
     */
    @Transactional
    public ResponseEntity<?> deleteNotification(Long notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElse(null);

        if (notification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Notification not found"));
        }

        notificationRepository.delete(notification);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Notification deleted successfully"));
    }

    /**
     * Get notifications for a specific P2P order
     */
    public ResponseEntity<?> getNotificationsByOrderId(Long orderId, String userId) {
        List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        // Filter to only show notifications belonging to the requesting user
        List<Notification> userNotifications = notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", userNotifications));
    }
}
