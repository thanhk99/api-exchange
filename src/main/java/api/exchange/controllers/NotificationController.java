package api.exchange.controllers;

import api.exchange.services.NotificationService;
import api.exchange.sercurity.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get user's notifications with pagination
     * GET /api/v1/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<?> getUserNotifications(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.getUserNotifications(userId, page, size);
    }

    /**
     * Get unread notifications
     * GET /api/v1/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.getUnreadNotifications(userId);
    }

    /**
     * Get unread notification count
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.getUnreadCount(userId);
    }

    /**
     * Mark a notification as read
     * PUT /api/v1/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.markAsRead(id, userId);
    }

    /**
     * Mark all notifications as read
     * PUT /api/v1/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.markAllAsRead(userId);
    }

    /**
     * Delete a notification
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.deleteNotification(id, userId);
    }

    /**
     * Get notifications for a specific P2P order
     * GET /api/v1/notifications/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getNotificationsByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return notificationService.getNotificationsByOrderId(orderId, userId);
    }
}
