package api.exchange.config;

import api.exchange.services.BinanceWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.net.URI;

@Configuration
public class WebSocketInitializer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${binance.api.urlSocket}")
    private String WebsocketUrl;

    @Bean
    public BinanceWebSocketClient binanceWebSocketClient() {
        BinanceWebSocketClient client = new BinanceWebSocketClient(
                URI.create(WebsocketUrl),
                messagingTemplate);
        client.connect();
        return client;
    }
}