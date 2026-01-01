package api.exchange.services;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import api.exchange.models.coinModel;
import api.exchange.repository.coinRepository;
import api.exchange.websocket.FuturesMarketWebSocket;
import jakarta.annotation.PostConstruct;

@Service
public class FuturesWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private coinRepository coinRepository;

    @Autowired
    private api.exchange.services.FuturesDataService futuresDataService;

    @Autowired
    private api.exchange.services.RedisCacheService redisCacheService;

    private FuturesMarketWebSocket futuresMarketWebSocket;

    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("🔧 Initializing Futures WebSocket Service...");
        try {
            // Fetch circulating supplies for Market Cap calculation
            Map<String, BigDecimal> supplyMap = coinRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            coinModel::getId, // "BTC", "ETH"
                            coin -> coin.getCirculatingSupply() != null ? coin.getCirculatingSupply() : BigDecimal.ZERO,
                            (v1, v2) -> v1 // Merge function (keep first)
                    ));
            System.out.println("💰 Loaded circulating supply for " + supplyMap.size() + " coins");

            // Connect to Binance Futures WebSocket
            URI uri = new URI("wss://fstream.binance.com/stream?streams=!ticker@arr/!markPrice@arr@1s");
            System.out.println("📡 Connecting to: " + uri);

            futuresMarketWebSocket = new FuturesMarketWebSocket(uri, messagingTemplate, objectMapper, supplyMap,
                    futuresDataService, redisCacheService);
            futuresMarketWebSocket.connect();

            System.out.println("✅ Futures WebSocket service initialized successfully!");
            System.out.println("📊 WebSocket endpoint: ws://localhost:8000/ws");
            System.out.println("📢 Topic: /topic/futures/markets");
            System.out.println("========================================");
        } catch (URISyntaxException e) {
            System.err.println("❌ Failed to initialize Futures WebSocket: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during WebSocket initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @jakarta.annotation.PreDestroy
    public void cleanup() {
        System.out.println("🛑 Closing Futures WebSocket Service...");
        if (futuresMarketWebSocket != null) {
            futuresMarketWebSocket.close();
        }
    }

    public void reconnect() {
        if (futuresMarketWebSocket != null && !futuresMarketWebSocket.isOpen()) {
            futuresMarketWebSocket.reconnect();
        }
    }
}
