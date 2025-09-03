package api.exchange.config;

import api.exchange.websocket.SpotWebSocketClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
public class WebSocketInitializer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${binance.api.urlSocket}")
    private String WebsocketUrl;

    @Bean
    public SpotWebSocketClient spotWebSocketClient() {
        try {
                SpotWebSocketClient client = new SpotWebSocketClient(
                    URI.create(WebsocketUrl), 
                    messagingTemplate, 
                    objectMapper
                );
                client.connect();
                return client;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create WebSocket client", e);
            }
        }
}