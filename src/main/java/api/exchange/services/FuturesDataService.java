package api.exchange.services;

import api.exchange.dtos.Response.KlinesFuturesResponse;
import api.exchange.models.FuturesKlineData1m;
import api.exchange.models.FuturesKlineData1h;
import api.exchange.repository.FuturesKlineData1mRepository;
import api.exchange.repository.FuturesKlineData1sRepository;
import api.exchange.repository.FuturesKlineData1hRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
 * Service để lấy dữ liệu kline từ Binance Futures API
 */
@Service
public class FuturesDataService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FuturesKlineData1mRepository futuresKlineData1mRepository;

    @Autowired
    private FuturesKlineData1hRepository futuresKlineData1hRepository;

    @Autowired
    private FuturesKlineData1sRepository futuresKlineData1sRepository;

    @Value("${binance.futures.api.url:https://fapi.binance.com/fapi/v1}")
    private String binanceFuturesApiUrl;

    // Các symbols được theo dõi (Top futures contracts)
    private static final List<String> SYMBOLS = Arrays.asList(
            "BTCUSDT"
    // "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
    // "ADAUSDT", "DOGEUSDT", "TRXUSDT", "DOTUSDT", "LTCUSDT",
    // "BCHUSDT", "LINKUSDT", "XLMUSDT", "ATOMUSDT", "UNIUSDT",
    // "AVAXUSDT", "NEARUSDT", "FILUSDT", "ICPUSDT", "ETCUSDT"
    );

    /**
     * Lấy dữ liệu kline từ Binance Futures API
     */
    public List<KlinesFuturesResponse> fetchKlineDataFromBinanceFutures(String symbol, String interval, int limit) {
        try {
            String url = String.format("%s/klines?symbol=%s&interval=%s&limit=%d",
                    binanceFuturesApiUrl, symbol, interval, limit);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return parseBinanceFuturesKlineData(jsonNode, symbol, interval);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching futures kline data from Binance for " + symbol + " " + interval + ": "
                    + e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Parse dữ liệu kline từ Binance Futures response
     */
    private List<KlinesFuturesResponse> parseBinanceFuturesKlineData(JsonNode jsonNode, String symbol,
            String interval) {
        List<KlinesFuturesResponse> klines = new ArrayList<>();

        for (JsonNode klineNode : jsonNode) {
            try {
                long openTime = klineNode.get(0).asLong();
                BigDecimal openPrice = new BigDecimal(klineNode.get(1).asText());
                BigDecimal highPrice = new BigDecimal(klineNode.get(2).asText());
                BigDecimal lowPrice = new BigDecimal(klineNode.get(3).asText());
                BigDecimal closePrice = new BigDecimal(klineNode.get(4).asText());
                BigDecimal volume = new BigDecimal(klineNode.get(5).asText());
                long closeTime = klineNode.get(6).asLong();

                // Kiểm tra xem nến đã đóng chưa
                boolean isClosed = System.currentTimeMillis() > closeTime;

                KlinesFuturesResponse kline = new KlinesFuturesResponse(
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
                System.err.println("❌ Error parsing futures kline data: " + e.getMessage());
            }
        }

        return klines;
    }

    /**
     * Lưu dữ liệu kline 1m vào database
     */
    public void saveKlineData1m(List<KlinesFuturesResponse> klines) {
        for (KlinesFuturesResponse kline : klines) {
            try {
                FuturesKlineData1m futuresKlineData = convertToFuturesKlineData1m(kline);

                // Kiểm tra xem nến đã tồn tại chưa
                FuturesKlineData1m existing = futuresKlineData1mRepository.findBySymbolAndStartTime(
                        futuresKlineData.getSymbol(), futuresKlineData.getStartTime());

                if (existing == null) {
                    futuresKlineData1mRepository.save(futuresKlineData);
                } else {
                    // Cập nhật nến hiện tại nếu có thay đổi
                    existing.setClosePrice(futuresKlineData.getClosePrice());
                    existing.setHighPrice(futuresKlineData.getHighPrice());
                    existing.setLowPrice(futuresKlineData.getLowPrice());
                    existing.setVolume(futuresKlineData.getVolume());
                    existing.setIsClosed(futuresKlineData.getIsClosed());
                    futuresKlineData1mRepository.save(existing);
                }
            } catch (Exception e) {
                System.err.println("❌ Error saving futures kline 1m data: " + e.getMessage());
            }
        }
    }

    /**
     * Lưu dữ liệu kline 1h vào database
     */
    public void saveKlineData1h(List<KlinesFuturesResponse> klines) {
        for (KlinesFuturesResponse kline : klines) {
            try {
                FuturesKlineData1h futuresKlineData = convertToFuturesKlineData1h(kline);

                // Kiểm tra xem nến đã tồn tại chưa
                FuturesKlineData1h existing = futuresKlineData1hRepository.findBySymbolAndStartTime(
                        futuresKlineData.getSymbol(), futuresKlineData.getStartTime());

                if (existing == null) {
                    futuresKlineData1hRepository.save(futuresKlineData);
                } else {
                    // Cập nhật nến hiện tại nếu có thay đổi
                    existing.setClosePrice(futuresKlineData.getClosePrice());
                    existing.setHighPrice(futuresKlineData.getHighPrice());
                    existing.setLowPrice(futuresKlineData.getLowPrice());
                    existing.setVolume(futuresKlineData.getVolume());
                    existing.setIsClosed(futuresKlineData.getIsClosed());
                    futuresKlineData1hRepository.save(existing);
                }
            } catch (Exception e) {
                System.err.println("❌ Error saving futures kline 1h data: " + e.getMessage());
            }
        }
    }

    /**
     * Lưu dữ liệu kline 1s vào database
     */
    public void saveKlineData1s(api.exchange.models.FuturesKlineData1s klineData) {
        try {
            futuresKlineData1sRepository.save(klineData);
        } catch (Exception e) {
            System.err.println("❌ Error saving futures kline 1s data: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi KlinesFuturesResponse thành FuturesKlineData1m
     */
    private FuturesKlineData1m convertToFuturesKlineData1m(KlinesFuturesResponse kline) {
        LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getStartTime()), ZoneId.systemDefault());
        LocalDateTime closeTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());

        return new FuturesKlineData1m(
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
     * Chuyển đổi KlinesFuturesResponse thành FuturesKlineData1h
     */
    private FuturesKlineData1h convertToFuturesKlineData1h(KlinesFuturesResponse kline) {
        LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getStartTime()), ZoneId.systemDefault());
        LocalDateTime closeTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());

        return new FuturesKlineData1h(
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
     * Lấy và lưu dữ liệu 1m cho tất cả symbols từ Binance Futures
     */
    public void fetchAndSaveAllKlineData1m() {
        for (String symbol : SYMBOLS) {
            try {
                List<KlinesFuturesResponse> klines = fetchKlineDataFromBinanceFutures(symbol, "1m", 1);

                if (!klines.isEmpty()) {
                    saveKlineData1m(klines);
                } else {
                    System.out.println("⚠️ No futures kline data available for " + symbol + " 1m from Binance");
                }

                // Thêm delay để tránh rate limit
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Error processing futures 1m data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    /**
     * Lấy và lưu dữ liệu 1h cho tất cả symbols từ Binance Futures
     */
    public void fetchAndSaveAllKlineData1h() {
        for (String symbol : SYMBOLS) {
            try {
                List<KlinesFuturesResponse> klines = fetchKlineDataFromBinanceFutures(symbol, "1h", 1);

                if (!klines.isEmpty()) {
                    saveKlineData1h(klines);
                } else {
                    System.out.println("⚠️ No futures kline data available for " + symbol + " 1h from Binance");
                }

                // Thêm delay để tránh rate limit
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("❌ Error processing futures 1h data for " + symbol + ": " + e.getMessage());
            }
        }
    }

    /**
     * Lấy danh sách symbols được theo dõi
     */
    public List<String> getTrackedSymbols() {
        return new ArrayList<>(SYMBOLS);
    }
}
