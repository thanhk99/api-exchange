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

    // Scheduled executor for broadcasting all markets
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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

    public FuturesMarketWebSocket(URI uri, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper,
            Map<String, BigDecimal> supplyMap) {
        super(uri);
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.supplyMap = supplyMap;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        // Subscribe to All Market Tickers and All Mark Prices
        String subscribeMessage = "{"
                + "\"method\": \"SUBSCRIBE\","
                + "\"params\": ["
                + "\"!ticker@arr\","
                + "\"!markPrice@arr@1s\""
                + "],"
                + "\"id\": 1"
                + "}";
        this.send(subscribeMessage);
        System.out.println("✅ Connected to Binance Futures WebSocket (Combined Streams)");

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
    }

    private long messageCount = 0;

    @Override
    public void onMessage(String message) {
        try {
            messageCount++;
            if (messageCount % 100 == 0) {
                System.out.println("📥 Received " + messageCount + " messages from Binance");
            }

            JsonNode jsonNode = objectMapper.readTree(message);

            if (jsonNode.has("stream") && jsonNode.has("data")) {
                String stream = jsonNode.get("stream").asText();
                JsonNode data = jsonNode.get("data");

                if (stream.equals("!ticker@arr")) {
                    processTickerData(data);
                } else if (stream.equals("!markPrice@arr@1s")) {
                    processMarkPriceData(data);
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
                    v.setPriceChange24h(new BigDecimal(node.path("p").asText("0")));
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

                // Only process symbols we care about
                String coinId = symbol.replace("USDT", "");
                if (!LOGO_URLS.containsKey(coinId))
                    continue;

                marketDataCache.compute(symbol, (k, v) -> {
                    if (v == null)
                        v = new FuturesMarketResponse();

                    v.setSymbol(symbol);
                    v.setMarkPrice(new BigDecimal(node.path("p").asText("0")));
                    v.setIndexPrice(new BigDecimal(node.path("i").asText("0")));
                    v.setFundingRate(new BigDecimal(node.path("r").asText("0")));
                    v.setNextFundingTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(node.path("T").asLong()),
                            ZoneId.systemDefault()));

                    // Defaults if missing
                    if (v.getLogoUrl() == null)
                        v.setLogoUrl(LOGO_URLS.getOrDefault(coinId, ""));
                    if (v.getLastPrice() == null)
                        v.setLastPrice(v.getMarkPrice()); // Fallback

                    return v;
                });
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Futures WebSocket closed: " + reason + " (code: " + code + ")");
        scheduler.shutdown();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ Futures WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }
}
