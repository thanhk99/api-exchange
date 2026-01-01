package api.exchange.services;

import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.models.SpotKlineData1m;
import api.exchange.models.SpotKlineData1h;
import api.exchange.models.coinModel;
import api.exchange.repository.SpotKlineData1mRepository;
import api.exchange.repository.SpotKlineData1hRepository;
import api.exchange.repository.coinRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service để lấy dữ liệu coin từ Binance API
 */
@Service
@EnableScheduling
public class CoinDataService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpotKlineData1mRepository spotKlineData1mRepository;

    @Autowired
    private SpotKlineData1hRepository spotKlineData1hRepository;

    @Autowired
    private coinRepository coinRepository;

    @Value("${binance.api.url}")
    private String binanceApiUrl;

    // Các symbols được theo dõi (Top 25 phổ biến)
    private static final List<String> SYMBOLS = Arrays.asList(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "TRXUSDT", "DOTUSDT", "SUIUSDT",
            "LTCUSDT", "BCHUSDT", "LINKUSDT", "XLMUSDT", "ATOMUSDT",
            "UNIUSDT", "AVAXUSDT", "NEARUSDT", "FILUSDT", "VETUSDT",
            "ALGOUSDT", "ICPUSDT", "SHIBUSDT", "TONUSDT", "ETCUSDT");

    // Mapping Symbol -> CoinGecko ID
    private static final java.util.Map<String, String> COIN_GECKO_IDS = java.util.Map.ofEntries(
            java.util.Map.entry("BTC", "bitcoin"),
            java.util.Map.entry("ETH", "ethereum"),
            java.util.Map.entry("BNB", "binancecoin"),
            java.util.Map.entry("SOL", "solana"),
            java.util.Map.entry("XRP", "ripple"),
            java.util.Map.entry("ADA", "cardano"),
            java.util.Map.entry("DOGE", "dogecoin"),
            java.util.Map.entry("TRX", "tron"),
            java.util.Map.entry("DOT", "polkadot"),
            java.util.Map.entry("SUI", "sui"),
            java.util.Map.entry("LTC", "litecoin"),
            java.util.Map.entry("BCH", "bitcoin-cash"),
            java.util.Map.entry("LINK", "chainlink"),
            java.util.Map.entry("XLM", "stellar"),
            java.util.Map.entry("ATOM", "cosmos"),
            java.util.Map.entry("UNI", "uniswap"),
            java.util.Map.entry("AVAX", "avalanche-2"),
            java.util.Map.entry("NEAR", "near"),
            java.util.Map.entry("FIL", "filecoin"),
            java.util.Map.entry("VET", "vechain"),
            java.util.Map.entry("ALGO", "algorand"),
            java.util.Map.entry("ICP", "internet-computer"),
            java.util.Map.entry("SHIB", "shiba-inu"),
            java.util.Map.entry("TON", "the-open-network"),
            java.util.Map.entry("ETC", "ethereum-classic"));

    // Cache circulating supply: Symbol -> Supply
    private java.util.Map<String, BigDecimal> circulatingSupplyCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Mapping logo URLs cho các coin (sử dụng CoinGecko CDN)
    private static final java.util.Map<String, String> LOGO_URLS = java.util.Map.ofEntries(
            java.util.Map.entry("BTC", "https://assets.coingecko.com/coins/images/1/large/bitcoin.png"),
            java.util.Map.entry("ETH", "https://assets.coingecko.com/coins/images/279/large/ethereum.png"),
            java.util.Map.entry("BNB", "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png"),
            java.util.Map.entry("SOL", "https://assets.coingecko.com/coins/images/4128/large/solana.png"),
            java.util.Map.entry("XRP", "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png"),
            java.util.Map.entry("ADA", "https://assets.coingecko.com/coins/images/975/large/cardano.png"),
            java.util.Map.entry("DOGE", "https://assets.coingecko.com/coins/images/5/large/dogecoin.png"),
            java.util.Map.entry("TRX", "https://assets.coingecko.com/coins/images/1094/large/tron-logo.png"),
            java.util.Map.entry("DOT", "https://assets.coingecko.com/coins/images/12171/large/polkadot.png"),
            java.util.Map.entry("SUI", "https://assets.coingecko.com/coins/images/26375/large/sui_asset.jpeg"),
            java.util.Map.entry("LTC", "https://assets.coingecko.com/coins/images/2/large/litecoin.png"),
            java.util.Map.entry("BCH", "https://assets.coingecko.com/coins/images/780/large/bitcoin-cash-circle.png"),
            java.util.Map.entry("LINK", "https://assets.coingecko.com/coins/images/877/large/chainlink-new-logo.png"),
            java.util.Map.entry("XLM",
                    "https://assets.coingecko.com/coins/images/100/large/Stellar_symbol_black_RGB.png"),
            java.util.Map.entry("ATOM", "https://assets.coingecko.com/coins/images/1481/large/cosmos_hub.png"),
            java.util.Map.entry("UNI", "https://assets.coingecko.com/coins/images/12504/large/uni.jpg"),
            java.util.Map.entry("AVAX",
                    "https://assets.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png"),
            java.util.Map.entry("NEAR", "https://assets.coingecko.com/coins/images/10365/large/near.jpg"),
            java.util.Map.entry("FIL", "https://assets.coingecko.com/coins/images/12817/large/filecoin.png"),
            java.util.Map.entry("VET", "https://assets.coingecko.com/coins/images/1167/large/VeChain-Logo-768x725.png"),
            java.util.Map.entry("ALGO", "https://assets.coingecko.com/coins/images/4380/large/download.png"),
            java.util.Map.entry("ICP",
                    "https://assets.coingecko.com/coins/images/14495/large/Internet_Computer_logo.png"),
            java.util.Map.entry("SHIB", "https://assets.coingecko.com/coins/images/11939/large/shiba.png"),
            java.util.Map.entry("TON", "https://assets.coingecko.com/coins/images/17980/large/ton_symbol.png"),
            java.util.Map.entry("ETC",
                    "https://assets.coingecko.com/coins/images/453/large/ethereum-classic-logo.png"));

    /**
     * Lấy danh sách tất cả coin đang theo dõi kèm thông tin giá
     */
    public List<coinModel> getAllCoins() {
        return coinRepository.findAll();
    }

    /**
     * Lấy dữ liệu kline từ Binance API
     */
    public List<KlinesSpotResponse> fetchKlineDataFromBinance(String symbol, String interval, int limit) {
        try {
            String url = String.format("%s/klines?symbol=%s&interval=%s&limit=%d",
                    binanceApiUrl, symbol, interval, limit);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return parseBinanceKlineData(jsonNode, symbol, interval);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching kline data from Binance for " + symbol + " " + interval + ": "
                    + e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Parse dữ liệu kline từ Binance response
     */
    private List<KlinesSpotResponse> parseBinanceKlineData(JsonNode jsonNode, String symbol, String interval) {
        List<KlinesSpotResponse> klines = new ArrayList<>();

        for (JsonNode klineNode : jsonNode) {
            try {
                long openTime = klineNode.get(0).asLong();
                BigDecimal openPrice = new BigDecimal(klineNode.get(1).asText());
                BigDecimal highPrice = new BigDecimal(klineNode.get(2).asText());
                BigDecimal lowPrice = new BigDecimal(klineNode.get(3).asText());
                BigDecimal closePrice = new BigDecimal(klineNode.get(4).asText());
                BigDecimal volume = new BigDecimal(klineNode.get(5).asText());
                long closeTime = klineNode.get(6).asLong();

                // Kiểm tra xem nến đã đóng chưa (close time đã qua thời điểm hiện tại chưa)
                boolean isClosed = System.currentTimeMillis() > closeTime;

                KlinesSpotResponse kline = new KlinesSpotResponse(
                        symbol,
                        openPrice,
                        closePrice,
                        highPrice,
                        lowPrice,
                        volume,
                        openTime,
                        closeTime,
                        interval,
                        isClosed);

                klines.add(kline);
            } catch (Exception e) {
                System.err.println("❌ Error parsing kline data: " + e.getMessage());
            }
        }

        return klines;
    }

    /**
     * Lưu dữ liệu kline 1m vào database
     */
    public void saveKlineData1m(List<KlinesSpotResponse> klines) {
        for (KlinesSpotResponse kline : klines) {
            try {
                SpotKlineData1m spotKlineData = convertToSpotKlineData1m(kline);

                // Kiểm tra xem nến đã tồn tại chưa
                SpotKlineData1m existing = spotKlineData1mRepository.findBySymbolAndStartTime(
                        spotKlineData.getSymbol(), spotKlineData.getStartTime());

                if (existing == null) {
                    spotKlineData1mRepository.save(spotKlineData);
                } else {
                    // Cập nhật nến hiện tại nếu có thay đổi
                    existing.setClosePrice(spotKlineData.getClosePrice());
                    existing.setHighPrice(spotKlineData.getHighPrice());
                    existing.setLowPrice(spotKlineData.getLowPrice());
                    existing.setVolume(spotKlineData.getVolume());
                    existing.setIsClosed(spotKlineData.getIsClosed());
                    spotKlineData1mRepository.save(existing);
                }
            } catch (Exception e) {
                System.err.println("❌ Error saving kline 1m data: " + e.getMessage());
            }
        }
    }

    /**
     * Lưu dữ liệu kline 1h vào database
     */
    public void saveKlineData1h(List<KlinesSpotResponse> klines) {
        for (KlinesSpotResponse kline : klines) {
            try {
                SpotKlineData1h spotKlineData = convertToSpotKlineData1h(kline);

                // Kiểm tra xem nến đã tồn tại chưa
                SpotKlineData1h existing = spotKlineData1hRepository.findBySymbolAndStartTime(
                        spotKlineData.getSymbol(), spotKlineData.getStartTime());

                if (existing == null) {
                    spotKlineData1hRepository.save(spotKlineData);
                } else {
                    // Cập nhật nến hiện tại nếu có thay đổi
                    existing.setClosePrice(spotKlineData.getClosePrice());
                    existing.setHighPrice(spotKlineData.getHighPrice());
                    existing.setLowPrice(spotKlineData.getLowPrice());
                    existing.setVolume(spotKlineData.getVolume());
                    existing.setIsClosed(spotKlineData.getIsClosed());
                    spotKlineData1hRepository.save(existing);
                }
            } catch (Exception e) {
                System.err.println("❌ Error saving kline 1h data: " + e.getMessage());
            }
        }
    }

    /**
     * Chuyển đổi KlinesSpotResponse thành SpotKlineData1m
     */
    private SpotKlineData1m convertToSpotKlineData1m(KlinesSpotResponse kline) {
        LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getStartTime()), ZoneId.systemDefault());
        LocalDateTime closeTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());

        return new SpotKlineData1m(
                kline.getSymbol(),
                kline.getOpenPrice(),
                kline.getClosePrice(),
                kline.getHighPrice(),
                kline.getLowPrice(),
                kline.getVolume(),
                startTime,
                closeTime,
                kline.isClosed());
    }

    /**
     * Chuyển đổi KlinesSpotResponse thành SpotKlineData1h
     */
    private SpotKlineData1h convertToSpotKlineData1h(KlinesSpotResponse kline) {
        LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getStartTime()), ZoneId.systemDefault());
        LocalDateTime closeTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());

        return new SpotKlineData1h(
                kline.getSymbol(),
                kline.getOpenPrice(),
                kline.getClosePrice(),
                kline.getHighPrice(),
                kline.getLowPrice(),
                kline.getVolume(),
                startTime,
                closeTime,
                kline.isClosed());
    }

    /**
     * Lấy và lưu dữ liệu 1m cho tất cả symbols từ Binance
     */
    public void fetchAndSaveAllKlineData1m() {
        for (String symbol : SYMBOLS) {
            try {
                List<KlinesSpotResponse> klines = fetchKlineDataFromBinance(symbol, "1m", 1);

                if (!klines.isEmpty()) {
                    saveKlineData1m(klines);
                } else {
                    System.out.println("⚠️ No kline data available for " + symbol + " 1m from Binance");
                }

                // Thêm delay để tránh rate limit
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Error processing 1m data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    /**
     * Lấy và lưu dữ liệu 1h cho tất cả symbols từ Binance
     */
    public void fetchAndSaveAllKlineData1h() {
        for (String symbol : SYMBOLS) {
            try {
                List<KlinesSpotResponse> klines = fetchKlineDataFromBinance(symbol, "1h", 1);

                if (!klines.isEmpty()) {
                    saveKlineData1h(klines);
                } else {
                    System.out.println("⚠️ No kline data available for " + symbol + " 1h from Binance");
                }

                // Thêm delay để tránh rate limit
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Error processing 1h data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    /**
     * Lấy thông tin coin từ Binance API
     */
    public void fetchAndSaveCoinInfo(String symbol) {
        try {
            String url = String.format("%s/ticker/24hr?symbol=%s", binanceApiUrl, symbol);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                String coinId = symbol.replace("USDT", "");
                coinModel coin = new coinModel();
                coin.setId(coinId);
                coin.setSymbol(symbol);

                coin.setCurrentPrice(new BigDecimal(jsonNode.get("lastPrice").asText()));
                coin.setPriceChange24h(new BigDecimal(jsonNode.get("priceChangePercent").asText()));

                // Set logo URL from mapping
                coin.setLogoUrl(LOGO_URLS.getOrDefault(coinId, ""));

                // Calculate Market Cap if circulating supply exists
                BigDecimal supply = getCirculatingSupply(coinId);
                if (supply != null && supply.compareTo(BigDecimal.ZERO) > 0) {
                    coin.setCirculatingSupply(supply);
                    coin.setMarketCap(coin.getCurrentPrice().multiply(supply));
                }

                coin.setLastUpdated(LocalDateTime.now());
                coinRepository.save(coin);

            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching coin info for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Lấy và lưu thông tin coin cho tất cả symbols
     */
    public void fetchAndSaveAllCoinInfo() {
        for (String symbol : SYMBOLS) {
            try {
                fetchAndSaveCoinInfo(symbol);

                // Thêm delay để tránh rate limit
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Error processing coin info for " + symbol + ": " + e.getMessage());
            }
        }
    }

    /**
     * Lấy danh sách symbols được theo dõi
     */
    public List<String> getTrackedSymbols() {
        return new ArrayList<>(SYMBOLS);
    }

    /**
     * Scheduled task để cập nhật thông tin coin mỗi phút
     */
    @Scheduled(fixedRate = 60000)
    public void updateCoinInfo() {
        fetchAndSaveAllCoinInfo();
    }

    /**
     * Scheduled task để cập nhật dữ liệu 1m mỗi phút
     */
    @Scheduled(fixedRate = 60000)
    public void updateKlineData1m() {
        fetchAndSaveAllKlineData1m();
    }

    /**
     * Scheduled task để cập nhật dữ liệu 1h mỗi giờ
     */
    @Scheduled(fixedRate = 3600000)
    public void updateKlineData1h() {
        fetchAndSaveAllKlineData1h();
    }

    /**
     * Lấy dữ liệu kline với interval tùy chỉnh
     */
    public List<KlinesSpotResponse> getCustomIntervalKline(String symbol, String interval, int limit) {
        return fetchKlineDataFromBinance(symbol, interval, limit);
    }

    /**
     * Lấy các intervals được hỗ trợ
     */
    public List<String> getSupportedIntervals() {
        return Arrays.asList("1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d", "1w",
                "1M");
    }

    /**
     * Lấy giá hiện tại của coin từ DB
     */
    public BigDecimal getCurrentPrice(String symbol) {
        String normalizedSymbol = symbol.toUpperCase().replace("USDT", "");
        if (normalizedSymbol.isEmpty() || "USDT".equals(normalizedSymbol)) {
            return BigDecimal.ONE;
        }

        return coinRepository.findById(normalizedSymbol)
                .map(coinModel::getCurrentPrice)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Lấy tỷ giá giữa hai đồng coin (from -> to).
     * Rate = PriceFrom / PriceTo
     */
    public BigDecimal getExchangeRate(String fromSymbol, String toSymbol) {
        BigDecimal priceFrom = getCurrentPrice(fromSymbol);
        BigDecimal priceTo = getCurrentPrice(toSymbol);

        if (priceTo.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return priceFrom.divide(priceTo, 8, RoundingMode.HALF_UP);
    }

    /**
     * Lấy circulating supply từ cache
     */
    public BigDecimal getCirculatingSupply(String symbol) {
        String coinId = symbol.toUpperCase().replace("USDT", "");
        return circulatingSupplyCache.getOrDefault(coinId, BigDecimal.ZERO);
    }

    /**
     * Fetch circulating supply từ CoinGecko và update cache + DB
     * Chạy mỗi 10 phút
     */
    @Scheduled(fixedRate = 600000)
    public void fetchAndSaveCoinSupply() {
        System.out.println("🔄 Fetching circulating supply from CoinGecko...");

        // Chia thành các batch nhỏ nếu cần, ở đây fetch hết 1 lần vì số lượng ít (25
        // coin)
        // CoinGecko API:
        // /coins/markets?vs_currency=usd&ids=bitcoin,ethereum,...&order=market_cap_desc&per_page=100&page=1&sparkline=false&locale=en

        try {
            String ids = String.join(",", COIN_GECKO_IDS.values());
            String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=" + ids
                    + "&order=market_cap_desc&per_page=100&page=1&sparkline=false&locale=en";

            // Lưu ý: Cần handle rate limit của CoinGecko (Free tier: 10-30 calls/min)
            // Có thể cần thêm header User-Agent để tránh bị block

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        String geckoId = node.path("id").asText();
                        BigDecimal supply = new BigDecimal(node.path("circulating_supply").asText("0"));

                        // Tìm symbol tương ứng với geckoId
                        String symbol = COIN_GECKO_IDS.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(geckoId))
                                .map(java.util.Map.Entry::getKey)
                                .findFirst()
                                .orElse(null);

                        if (symbol != null) {
                            // Update cache
                            circulatingSupplyCache.put(symbol, supply);

                            // Update DB (optional, nếu muốn persist)
                            coinRepository.findById(symbol).ifPresent(coin -> {
                                coin.setCirculatingSupply(supply);
                                if (coin.getCurrentPrice() != null) {
                                    coin.setMarketCap(coin.getCurrentPrice().multiply(supply));
                                }
                                coinRepository.save(coin);
                            });
                        }
                    }
                }
                System.out.println("✅ Circulating supply updated successfully.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching circulating supply: " + e.getMessage());
        }
    }

    /**
     * Khởi tạo cache từ DB khi start app
     */
    @jakarta.annotation.PostConstruct
    public void initSupplyCache() {
        List<coinModel> coins = coinRepository.findAll();
        for (coinModel coin : coins) {
            if (coin.getCirculatingSupply() != null) {
                circulatingSupplyCache.put(coin.getId(), coin.getCirculatingSupply());
            }
        }
    }
}