package api.exchange.websocket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import api.exchange.dtos.Response.CoinSpotResponse;
import api.exchange.proto.MarketDataProto;

public class SpotMarketWebSocket extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private final api.exchange.services.MarketDataProcessor marketDataProcessor;

    public SpotMarketWebSocket(URI uri, SimpMessagingTemplate messagingTemplate,
            api.exchange.services.MarketDataProcessor marketDataProcessor) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.marketDataProcessor = marketDataProcessor;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.send("""
                {
                    "method": "SUBSCRIBE",
                    "params": [
                        "btcusdt@ticker", "ethusdt@ticker", "bnbusdt@ticker", "solusdt@ticker", "xrpusdt@ticker",
                        "adausdt@ticker", "dogeusdt@ticker", "trxusdt@ticker", "dotusdt@ticker", "suiusdt@ticker",
                        "ltcusdt@ticker", "bchusdt@ticker", "linkusdt@ticker", "xlmusdt@ticker", "atomusdt@ticker",
                        "uniusdt@ticker", "avaxusdt@ticker", "nearusdt@ticker", "filusdt@ticker", "vetusdt@ticker",
                        "algousdt@ticker", "icpusdt@ticker", "shibusdt@ticker", "tonusdt@ticker", "etcusdt@ticker"
                    ],
                    "id": 1
                }
                """);
    }

    @Override
    public void onMessage(String message) {
        try {
            CoinSpotResponse filteredData = marketDataProcessor.processMessage(message);

            if (filteredData != null) {
                // 1. Send JSON (Legacy)
                messagingTemplate.convertAndSend("/topic/spot-prices", filteredData);

                // 2. Send Binary (Protobuf)
                byte[] binaryData = MarketDataProto.serializeTicker(
                        filteredData.getSymbol(),
                        filteredData.getPrice() != null ? filteredData.getPrice().toString() : "0",
                        filteredData.getChangePercent() != null ? filteredData.getChangePercent().toString() : "0",
                        filteredData.getHigh24h() != null ? filteredData.getHigh24h().toString() : "0",
                        filteredData.getLow24h() != null ? filteredData.getLow24h().toString() : "0",
                        filteredData.getVolume() != null ? filteredData.getVolume().toString() : "0",
                        filteredData.getTimestamp() != null ? filteredData.getTimestamp() : 0L,
                        filteredData.getMarketCap() != null ? filteredData.getMarketCap().toString() : "0");
                messagingTemplate.convertAndSend("/topic/spot-prices-binary", binaryData);
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
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
