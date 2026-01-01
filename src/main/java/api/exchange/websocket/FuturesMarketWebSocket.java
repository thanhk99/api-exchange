package api.exchange.websocket;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import api.exchange.dtos.Response.FuturesMarketResponse;

public class FuturesMarketWebSocket extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, BigDecimal> supplyMap;

    // Store latest data for all symbols
    private final ConcurrentHashMap<String, FuturesMarketResponse> marketDataCache = new ConcurrentHashMap<>();

    // Track volume per second for each symbol
    private final ConcurrentHashMap<String, BigDecimal> volumePerSecond = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> currentSecondVolume = new ConcurrentHashMap<>();

    // Scheduled executor for broadcasting all markets
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService volumeResetScheduler = Executors.newSingleThreadScheduledExecutor();

    // Logo URLs
    private static final Map<String, String> LOGO_URLS = Map.ofEntries(
            Map.entry("BTC", "https://assets.coingecko.com/coins/images/1/large/bitcoin.png"),
            Map.entry("ETH", "https://assets.coingecko.com/coins/images/279/large/ethereum.png"),
            Map.entry("BNB", "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png"),
            Map.entry("SOL", "https://assets.coingecko.com/coins/images/4128/large/solana.png"),
            Map.entry("XRP", "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png"),
            Map.entry("ADA", "https://assets.coingecko.com/coins/images/975/large/cardano.png"),
            Map.entry("DOGE", "https://assets.coingecko.com/coins/images/5/large/dogecoin.png"),
            Map.entry("TRX", "https://assets.coingecko.com/coins/images/1094/large/tron-logo.png"),
            Map.entry("DOT", "https://assets.coingecko.com/coins/images/12171/large/polkadot.png"),
            Map.entry("LTC", "https://assets.coingecko.com/coins/images/2/large/litecoin.png"),
            Map.entry("BCH", "https://assets.coingecko.com/coins/images/780/large/bitcoin-cash-circle.png"),
            Map.entry("LINK", "https://assets.coingecko.com/coins/images/877/large/chainlink-new-logo.png"),
            Map.entry("XLM", "https://assets.coingecko.com/coins/images/100/large/Stellar_symbol_black_RGB.png"),
            Map.entry("ATOM", "https://assets.coingecko.com/coins/images/1481/large/cosmos_hub.png"),
            Map.entry("UNI", "https://assets.coingecko.com/coins/images/12504/large/uni.jpg"),
            Map.entry("AVAX",
                    "https://assets.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png"),
            Map.entry("NEAR", "https://assets.coingecko.com/coins/images/10365/large/near.jpg"),
            Map.entry("FIL", "https://assets.coingecko.com/coins/images/12817/large/filecoin.png"),
            Map.entry("ICP", "https://assets.coingecko.com/coins/images/14495/large/Internet_Computer_logo.png"),
            Map.entry("ETC", "https://assets.coingecko.com/coins/images/453/large/ethereum-classic-logo.png"));

    private final api.exchange.services.FuturesDataService futuresDataService;
    private final api.exchange.services.RedisCacheService redisCacheService;

    public FuturesMarketWebSocket(URI uri, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper,
            Map<String, BigDecimal> supplyMap, api.exchange.services.FuturesDataService futuresDataService,
            api.exchange.services.RedisCacheService redisCacheService) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.supplyMap = supplyMap;
        this.futuresDataService = futuresDataService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        // Build subscription params with ticker, mark price, and aggregated trades for
        // tracked symbols
        StringBuilder params = new StringBuilder();
        params.append("\"!ticker@arr\",");
        params.append("\"!markPrice@arr@1s\"");

        // Add aggregated trade streams for each tracked symbol
        for (String coinId : LOGO_URLS.keySet()) {
            String symbol = coinId + "USDT";
            params.append(",\"").append(symbol.toLowerCase()).append("@aggTrade\"");
        }

        String subscribeMessage = "{"
                + "\"method\": \"SUBSCRIBE\","
                + "\"params\": [" + params.toString() + "],"
                + "\"id\": 1"
                + "}";
        this.send(subscribeMessage);
        System.out.println("✅ Connected to Binance Futures WebSocket (Combined Streams + Trades)");

        // Broadcast all markets every second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!marketDataCache.isEmpty()) {
                    List<FuturesMarketResponse> allMarkets = new ArrayList<>(marketDataCache.values());
                    messagingTemplate.convertAndSend("/topic/futures/markets", allMarkets);
                }
            } catch (Exception e) {
                System.err.println("❌ Error broadcasting all markets: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        // Reset volume counters every second
        volumeResetScheduler.scheduleAtFixedRate(() -> {
            try {
                // Copy current second volume to volumePerSecond for use in 1s klines
                volumePerSecond.clear();
                volumePerSecond.putAll(currentSecondVolume);
                // Clear current second volume for next second
                currentSecondVolume.clear();
            } catch (Exception e) {
                System.err.println("❌ Error resetting volume counters: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            if (jsonNode.has("stream") && jsonNode.has("data")) {
                String stream = jsonNode.get("stream").asText();
                JsonNode data = jsonNode.get("data");

                if (stream.equals("!ticker@arr")) {
                    processTickerData(data);
                } else if (stream.equals("!markPrice@arr@1s")) {
                    processMarkPriceData(data);
                } else if (stream.endsWith("@aggTrade")) {
                    processAggTradeData(data);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing Futures WebSocket message: " + e.getMessage());
        }
    }

    private void processTickerData(JsonNode dataArray) {
        if (dataArray.isArray()) {
            for (JsonNode node : dataArray) {
                String symbol = node.path("s").asText().toUpperCase();

                // Only process symbols we care about (from LOGO_URLS keys + USDT)
                String coinId = symbol.replace("USDT", "");
                if (!LOGO_URLS.containsKey(coinId))
                    continue;

                marketDataCache.compute(symbol, (k, v) -> {
                    if (v == null)
                        v = new FuturesMarketResponse();

                    v.setSymbol(symbol);
                    v.setLastPrice(new BigDecimal(node.path("c").asText("0")));
                    v.setPriceChange24h(new BigDecimal(node.path("P").asText("0")));
                    v.setPriceChangePercent(new BigDecimal(node.path("P").asText("0")));
                    v.setHighPrice24h(new BigDecimal(node.path("h").asText("0")));
                    v.setLowPrice24h(new BigDecimal(node.path("l").asText("0")));
                    v.setVolume24h(new BigDecimal(node.path("v").asText("0"))); // Base Volume
                    v.setQuoteVolume24h(new BigDecimal(node.path("q").asText("0"))); // Quote Volume
                    v.setTimestamp(node.path("E").asLong());

                    // Calculate Market Cap
                    BigDecimal supply = supplyMap.getOrDefault(coinId, BigDecimal.ZERO);
                    v.setMarketCap(v.getLastPrice().multiply(supply));

                    // Defaults if missing
                    if (v.getLogoUrl() == null)
                        v.setLogoUrl(LOGO_URLS.getOrDefault(coinId, ""));
                    if (v.getOpenInterest() == null)
                        v.setOpenInterest(BigDecimal.ZERO);

                    return v;
                });
            }
        }
    }

    private void processMarkPriceData(JsonNode dataArray) {
        if (dataArray.isArray()) {
            for (JsonNode node : dataArray) {
                String symbol = node.path("s").asText().toUpperCase();
                BigDecimal price = new BigDecimal(node.path("p").asText("0"));

                // Update cache if exists
                if (marketDataCache.containsKey(symbol)) {
                    FuturesMarketResponse response = marketDataCache.get(symbol);
                    response.setMarkPrice(price);
                    response.setIndexPrice(new BigDecimal(node.path("i").asText("0")));
                    response.setFundingRate(new BigDecimal(node.path("r").asText("0")));
                    response.setNextFundingTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(node.path("T").asLong()),
                            ZoneId.systemDefault()));
                }

                // Process 1s kline for tracked symbols
                String coinId = symbol.replace("USDT", "");
                if (LOGO_URLS.containsKey(coinId)) {
                    process1sKline(symbol, price);
                }
            }
        }
    }

    private void process1sKline(String symbol, BigDecimal price) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Get volume for this second (default to 0 if no trades)
            BigDecimal volume = volumePerSecond.getOrDefault(symbol, BigDecimal.ZERO);

            // Tạo kline 1s với volume thực
            api.exchange.models.FuturesKlineData1s kline1s = new api.exchange.models.FuturesKlineData1s(
                    symbol, price, volume, now);

            // Lưu vào database (async để không block websocket)
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                futuresDataService.saveKlineData1s(kline1s);
            });

            // Invalidate Redis Cache để lần tới API gọi sẽ lấy dữ liệu mới nhất từ DB
            redisCacheService.invalidate1sKlineCache(symbol);

            // Broadcast tới topic riêng cho kline 1s
            // Format: /topic/futures/kline/1s/{symbol}
            String topic = "/topic/futures/kline/1s/" + symbol.toLowerCase();

            Map<String, Object> klineData = new java.util.HashMap<>();
            klineData.put("s", symbol);
            klineData.put("o", price);
            klineData.put("c", price);
            klineData.put("h", price);
            klineData.put("l", price);
            klineData.put("v", volume);
            klineData.put("t", now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            klineData.put("i", "1s");

            messagingTemplate.convertAndSend(topic, klineData);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("destroyed") || msg.contains("closed"))) {
                // Silently ignore during shutdown
                return;
            }
            System.err.println("Error processing 1s kline for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Process aggregated trade data to accumulate volume per second
     */
    private void processAggTradeData(JsonNode data) {
        try {
            String symbol = data.path("s").asText().toUpperCase();
            BigDecimal quantity = new BigDecimal(data.path("q").asText("0"));

            // Accumulate volume for current second
            currentSecondVolume.compute(symbol, (k, v) -> v == null ? quantity : v.add(quantity));

        } catch (Exception e) {
            System.err.println("❌ Error processing aggTrade data: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Futures WebSocket closed: " + reason + " (code: " + code + ")");
        scheduler.shutdown();
        volumeResetScheduler.shutdown();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ Futures WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }
}
