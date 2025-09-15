package api.exchange.websocket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SpotMarketWebSocket extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public SpotMarketWebSocket(URI uri, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ WebSocket connection closed: " + reason + " (code: " + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }
}
