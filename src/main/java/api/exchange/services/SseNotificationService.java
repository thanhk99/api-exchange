package api.exchange.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import api.exchange.models.Notification;

@Service
public class SseNotificationService {
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    public SseEmitter subscribe(String userId) {
        // Remove existing emitter if any
        completeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);

        // Setup completion and timeout callbacks
        emitter.onCompletion(() -> completeEmitter(userId));
        emitter.onTimeout(() -> completeEmitter(userId));

        // Send welcome event
        sendWelcomeEvent(emitter, userId);

        return emitter;
    }

    public void sendNotification(String userId, Notification message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            sendEvent(emitter, "notification", message);
        }
    }

    public void broadcast(Notification message) {
        emitters.forEach((userId, emitter) -> {
            sendEvent(emitter, "notification", message);
        });
    }

    public void completeEmitter(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    private void sendWelcomeEvent(SseEmitter emitter, String userId) {
        executor.execute(() -> {
            try {
                String welcomeMessage = "SSE connection established for user: " + userId;
                emitter.send(SseEmitter.event()
                        .name("connected")
                        .data(welcomeMessage));
            } catch (Exception e) {
                // log.error("Error sending welcome event", e); // Ensure logging is enabled
                completeEmitter(userId);
            }
        });
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                // Handle exception (e.g., client disconnected)
                completeEmitter(findUserIdByEmitter(emitter));
            }
        });
    }

    private String findUserIdByEmitter(SseEmitter targetEmitter) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getValue().equals(targetEmitter))
                .map(entry -> entry.getKey())
                .findFirst()
                .orElse(null);
    }

    // Helper method to get all connected users
    public List<String> getConnectedUsers() {
        return new ArrayList<>(emitters.keySet());
    }

    // Helper method to check if user is connected
    public boolean isUserConnected(String userId) {
        return emitters.containsKey(userId);
    }
}
