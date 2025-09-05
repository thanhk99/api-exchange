package api.exchange.config;

import api.exchange.websocket.SpotMarketWebSocket;
import api.exchange.websocket.SpotPriceCoinSocket;

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
    public SpotMarketWebSocket SpotMarketWebSocket() {
        try {
                SpotMarketWebSocket client = new SpotMarketWebSocket(
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

    @Bean
    public SpotPriceCoinSocket SpotPriceCoinSocket() {
        try {
                SpotPriceCoinSocket client = new SpotPriceCoinSocket(
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