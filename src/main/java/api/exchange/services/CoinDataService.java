package api.exchange.services;

import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.models.SpotKlineData1m;
import api.exchange.models.SpotKlineData1h;
import api.exchange.models.coinModel;
import api.exchange.models.priceHistoryModel;
import api.exchange.repository.SpotKlineData1mRepository;
import api.exchange.repository.SpotKlineData1hRepository;
import api.exchange.repository.coinRepository;
import api.exchange.repository.priceHistoryRepository;
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

    @Autowired
    private priceHistoryRepository priceHistoryRepository;

    @Value("${binance.api.url}")
    private String binanceApiUrl;

    // Các symbols được theo dõi
    private static final List<String> SYMBOLS = Arrays.asList("BTCUSDT", "ETHUSDT", "SOLUSDT");

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

                coinModel coin = new coinModel();
                coin.setId(symbol.replace("USDT", ""));
                coin.setSymbol(symbol);

                coin.setCurrentPrice(new BigDecimal(jsonNode.get("lastPrice").asText()));
                coin.setPriceChange24h(new BigDecimal(jsonNode.get("priceChangePercent").asText()));

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
     * Lấy lịch sử giá từ database
     */
    public List<priceHistoryModel> getListHisCoin(coinModel entity) {
        return priceHistoryRepository.findTop288BySymbolOrderByTimestampDesc(entity.getSymbol());
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
}