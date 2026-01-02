package api.exchange.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import api.exchange.models.Notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class SseNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private static final long HEARTBEAT_INTERVAL = 30; // 30 seconds

    public SseNotificationService() {
        this.taskExecutor = new ThreadPoolTaskExecutor();
        this.taskExecutor.setCorePoolSize(5);
        this.taskExecutor.setMaxPoolSize(20);
        this.taskExecutor.setQueueCapacity(100);
        this.taskExecutor.setThreadNamePrefix("sse-pumper-");
        this.taskExecutor.initialize();
    }

    @PostConstruct
    public void init() {
        startHeartbeat();
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        taskExecutor.shutdown();
        emitters.values().forEach(SseEmitter::complete);
        emitters.clear();
    }

    public SseEmitter subscribe(String userId) {
        log.info("User {} subscribing to SSE", userId);

        // Remove existing emitter if any to avoid leaks
        completeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Setup completion, timeout and error callbacks
        emitter.onCompletion(() -> {
            log.trace("SSE completed for user {}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.trace("SSE timeout for user {}", userId);
            completeEmitter(userId);
        });
        emitter.onError((ex) -> {
            log.debug("SSE error for user {}: {}", userId, ex.getMessage());
            completeEmitter(userId);
        });

        emitters.put(userId, emitter);

        // Send welcome event
        sendWelcomeEvent(emitter, userId);

        return emitter;
    }

    public void sendNotification(String userId, Notification message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            sendEvent(userId, emitter, "notification", message);
        } else {
            log.trace("No active SSE connection for user {}", userId);
        }
    }

    public void broadcast(Notification message) {
        log.trace("Broadcasting notification to all connected users");
        emitters.forEach((userId, emitter) -> {
            sendEvent(userId, emitter, "notification", message);
        });
    }

    public void completeEmitter(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.trace("Error completing emitter for user {}: {}", userId, e.getMessage());
            }
        }
    }

    private void sendWelcomeEvent(SseEmitter emitter, String userId) {
        sendEvent(userId, emitter, "connected", "SSE connection established for user: " + userId);
    }

    private void sendEvent(String userId, SseEmitter emitter, String eventName, Object data) {
        taskExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                completeEmitter(userId);
            }
        });
    }

    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitters.forEach((userId, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .comment("heartbeat"));
                    } catch (Exception e) {
                        completeEmitter(userId);
                    }
                });
            } catch (Exception e) {
                log.trace("Error in heartbeat loop: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    public List<String> getConnectedUsers() {
        return new ArrayList<>(emitters.keySet());
    }

    public boolean isUserConnected(String userId) {
        return emitters.containsKey(userId);
    }
}
