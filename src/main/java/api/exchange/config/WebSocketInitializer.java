package api.exchange.config;

import api.exchange.websocket.SpotMarketWebSocket;
import api.exchange.websocket.SpotPriceCoinSocket;
import api.exchange.services.RingBufferService;

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

    @Autowired
    private RingBufferService ringBufferService;

    @Value("${binance.api.urlSocket}")
    private String WebsocketUrl;

    @Autowired
    private api.exchange.services.CoinDataService coinDataService;

    @Autowired
    private api.exchange.services.MarketDataProcessor marketDataProcessor;

    @Bean
    public SpotMarketWebSocket SpotMarketWebSocket() {
        try {
            SpotMarketWebSocket client = new SpotMarketWebSocket(
                    URI.create(WebsocketUrl),
                    messagingTemplate,
                    marketDataProcessor);
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
                    marketDataProcessor,
                    ringBufferService);

            // Seed dữ liệu ban đầu cho các symbol quan trọng
            seedInitialKlines(client);

            client.connect();
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WebSocket client", e);
        }
    }

    private void seedInitialKlines(SpotPriceCoinSocket client) {
        String[] mainSymbols = { "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT", "DOGEUSDT" };
        for (String symbol : mainSymbols) {
            try {
                java.util.List<api.exchange.dtos.Response.KlinesSpotResponse> klines = coinDataService
                        .fetchKlineDataFromBinance(symbol, "1s", 72);
                if (klines != null && !klines.isEmpty()) {
                    for (api.exchange.dtos.Response.KlinesSpotResponse kline : klines) {
                        ringBufferService.addKlineData(kline);
                    }
                    System.out.println("✅ Seeded " + klines.size() + " klines for " + symbol);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to seed klines for " + symbol + ": " + e.getMessage());
            }
        }
    }
}