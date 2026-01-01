package api.exchange.services;

import api.exchange.dtos.Response.CoinSpotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service chuyên biệt để xử lý dữ liệu thị trường (Parsing, Filtering,
 * Calculation)
 * Tách biệt hoàn toàn khỏi logic kết nối WebSocket.
 */
@Service
public class MarketDataProcessor {

    @Autowired
    private CoinDataService coinDataService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Parse message từ Binance và chuyển đổi thành model CoinSpotResponse
     */
    public CoinSpotResponse processMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // Case 1: Stream Combined (Multi-stream)
            if (jsonNode.has("stream") && jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                return parseTickerData(data);
            }
            // Case 2: Single Stream
            else if (jsonNode.has("e") && jsonNode.has("s")) {
                return parseTickerData(jsonNode);
            }
            // Case 3: Subscription Confirmation (Ignore)
            else if (jsonNode.has("result") && jsonNode.has("id")) {
                // Log debug if needed
                return null;
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ MarketDataProcessor Error: " + e.getMessage());
            return null;
        }
    }

    private CoinSpotResponse parseTickerData(JsonNode node) {
        String symbol = node.path("s").asText();
        BigDecimal price = new BigDecimal(node.path("c").asText("0"));

        // Calculate Market Cap
        BigDecimal supply = coinDataService.getCirculatingSupply(symbol);
        BigDecimal marketCap = price.multiply(supply);

        return new CoinSpotResponse(
                symbol,
                price,
                new BigDecimal(node.path("P").asText("0")), // Price Change Percent
                new BigDecimal(node.path("h").asText("0")), // High
                new BigDecimal(node.path("l").asText("0")), // Low
                new BigDecimal(node.path("v").asText("0")), // Volume
                node.path("E").asLong(), // Event Time
                marketCap);
    }

    /**
     * Parse message kline từ Binance
     */
    public api.exchange.dtos.Response.KlinesSpotResponse processKlineMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            // Case 1: Stream Combined (Multi-stream)
            if (jsonNode.has("stream") && jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                if (data.has("k")) {
                    return parseKlineData(data.get("k"), data.get("s").asText());
                }
            }
            // Case 2: Single Stream
            else if (jsonNode.has("e") && "kline".equals(jsonNode.get("e").asText()) && jsonNode.has("k")) {
                return parseKlineData(jsonNode.get("k"), jsonNode.get("s").asText());
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ MarketDataProcessor Kline Error: " + e.getMessage());
            return null;
        }
    }

    private api.exchange.dtos.Response.KlinesSpotResponse parseKlineData(JsonNode klineNode, String symbol) {
        return new api.exchange.dtos.Response.KlinesSpotResponse(
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
}
