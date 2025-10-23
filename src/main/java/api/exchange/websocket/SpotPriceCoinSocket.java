package api.exchange.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.services.RingBufferService;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;

public class SpotPriceCoinSocket extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RingBufferService ringBufferService;

    public SpotPriceCoinSocket(URI uri, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper,
            RingBufferService ringBufferService) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
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
                                "solusdt@kline_1s"
                            ],
                            "id": 1
                        }
                """);
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // Xử lý dữ liệu kline stream
            if (jsonNode.has("stream") && jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                if (data.has("k")) {
                    JsonNode klineData = data.get("k");
                    KlinesSpotResponse klineResponse = processKlineData(klineData, data.get("s").asText());

                    // Thêm dữ liệu vào RingBuffer
                    ringBufferService.addKlineData(klineResponse);

                    // Gửi qua WebSocket
                    messagingTemplate.convertAndSend("/topic/kline-data", klineResponse);
                }
            }
            // Xử lý dữ liệu kline single stream
            else if (jsonNode.has("e") && "kline".equals(jsonNode.get("e").asText()) && jsonNode.has("k")) {
                JsonNode klineData = jsonNode.get("k");
                KlinesSpotResponse klineResponse = processKlineData(klineData, jsonNode.get("s").asText());

                // Thêm dữ liệu vào RingBuffer
                ringBufferService.addKlineData(klineResponse);

                // Gửi qua WebSocket
                messagingTemplate.convertAndSend("/topic/kline-data", klineResponse);
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

    private KlinesSpotResponse processKlineData(JsonNode klineNode, String symbol) {
        return new KlinesSpotResponse(
                symbol,
                new BigDecimal(klineNode.path("o").asText("0")), // Open price
                new BigDecimal(klineNode.path("c").asText("0")), // Close price
                new BigDecimal(klineNode.path("h").asText("0")), // High price
                new BigDecimal(klineNode.path("l").asText("0")), // Low price
                new BigDecimal(klineNode.path("v").asText("0")), // Volume
                klineNode.path("t").asLong(), // Start time
                klineNode.path("T").asLong(), // Close time
                klineNode.path("i").asText(), // Interval
                klineNode.path("x").asBoolean() // Is closed
        );
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