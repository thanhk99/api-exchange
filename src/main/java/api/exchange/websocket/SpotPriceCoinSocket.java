package api.exchange.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.services.MarketDataProcessor;
import api.exchange.services.RingBufferService;

import java.net.URI;

public class SpotPriceCoinSocket extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataProcessor marketDataProcessor;
    private final RingBufferService ringBufferService;

    public SpotPriceCoinSocket(URI uri, SimpMessagingTemplate messagingTemplate,
            MarketDataProcessor marketDataProcessor,
            RingBufferService ringBufferService) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.marketDataProcessor = marketDataProcessor;
        this.ringBufferService = ringBufferService;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        this.send("""
                        {
                            "method": "SUBSCRIBE",
                            "params": [
                                "btcusdt@kline_1s",
                                "ethusdt@kline_1s",
                                "solusdt@kline_1s",
                                "bnbusdt@kline_1s",
                                "xrpusdt@kline_1s",
                                "adausdt@kline_1s",
                                "dogeusdt@kline_1s"
                            ],
                            "id": 1
                        }
                """);
        System.out.println("✅ SpotPriceCoinSocket (Kline 1s) connected");
    }

    @Override
    public void onMessage(String message) {
        try {
            KlinesSpotResponse klineResponse = marketDataProcessor.processKlineMessage(message);

            if (klineResponse != null) {
                // Thêm dữ liệu vào RingBuffer
                ringBufferService.addKlineData(klineResponse);

                // Gửi qua WebSocket
                messagingTemplate.convertAndSend("/topic/kline-data", klineResponse);
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("destroyed") || msg.contains("closed"))) {
                // Silently ignore during shutdown
                return;
            }
            System.err.println("❌ SpotPriceCoinSocket Error: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ SpotPriceCoinSocket closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ SpotPriceCoinSocket error: " + ex.getMessage());
    }
}