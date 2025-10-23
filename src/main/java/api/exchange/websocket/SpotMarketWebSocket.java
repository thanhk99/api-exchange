package api.exchange.websocket;

import java.math.BigDecimal;
import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import api.exchange.dtos.Response.CoinSpotResponse;

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
        this.send("""
                {
                    "method": "SUBSCRIBE",
                    "params": [
                        "btcusdt@ticker",
                        "ethusdt@ticker",
                        "solusdt@ticker",
                        "xrpusdt@ticker",
                        "okbusdt@ticker",
                        "bnbusdt@ticker",
                        "adausdt@ticker",
                        "dotusdt@ticker"
                    ],
                    "id": 1
                }
                """);
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            // Xử lý stream data từ Binance
            if (jsonNode.has("stream") && jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                CoinSpotResponse filteredData = filterData(data);
                messagingTemplate.convertAndSend("/topic/spot-prices", filteredData);
            } else if (jsonNode.has("e") && jsonNode.has("s")) { // single stream 24hrTicker
                CoinSpotResponse filteredData = filterData(jsonNode);
                messagingTemplate.convertAndSend("/topic/spot-prices", filteredData);
            }
            // Xử lý response confirm subscription
            else if (jsonNode.has("result") && jsonNode.has("id")) {
                System.out.println("✅ Subscription confirmed: " + jsonNode.toString());
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private CoinSpotResponse filterData(JsonNode node) {
        String symbol = node.path("s").asText();

        return new CoinSpotResponse(
                symbol,
                new BigDecimal(node.path("c").asText("0")),
                new BigDecimal(node.path("P").asText("0")),
                new BigDecimal(node.path("h").asText("0")),
                new BigDecimal(node.path("l").asText("0")),
                new BigDecimal(node.path("v").asText("0")),
                node.path("E").asLong());
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
