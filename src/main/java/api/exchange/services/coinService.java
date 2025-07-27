package api.exchange.services;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import api.exchange.models.coinModel;
import api.exchange.models.priceHistoryModel;
import api.exchange.repository.coinRepository;
import api.exchange.repository.priceHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class coinService {
    @Autowired
    private coinRepository coinRepository;
    @Autowired
    private priceHistoryRepository priceHistoryRepository;
    private final RestTemplate restTemplate;

    private static final List<String> TRACKED_SYMBOLS = List.of("BTC", "ETH", "BNB");
    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3";
    private static final String API_RATE_COIN_FIAT = "https://api.coingecko.com/api/v3/simple/price?ids=tether&vs_currencies=usd,vnd";

    // @PostConstruct
    public void fetchInitialData() {
        log.info("Starting initial data fetch for {} coins", TRACKED_SYMBOLS.size());

        TRACKED_SYMBOLS.forEach(symbol -> {
            try {
                // Lấy thông tin cơ bản của coin
                fetchAndSaveCoinInfo(symbol);

                // Lấy lịch sử giá 24h với khung 5 phút
                fetchAndSavePriceHistory(symbol, "5m", 288);

                log.info("Completed initial data fetch for {}", symbol);
            } catch (Exception e) {
                log.error("Failed to fetch initial data for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Initial data fetch failed for " + symbol, e);
            }
        });
    }

    private void fetchAndSaveCoinInfo(String symbol) {

        String url = BINANCE_API_URL + "/ticker/24hr?symbol=" + symbol + "USDT";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Invalid API response for symbol: " + symbol);
            }

            Map<String, Object> apiData = response.getBody();
            coinModel coin = new coinModel();
            coin.setId(symbol);
            coin.setName((String) apiData.get("symbol"));
            coin.setCurrentPrice(new BigDecimal(apiData.get("lastPrice").toString()));
            coin.setPriceChange24h(new BigDecimal(apiData.get("priceChangePercent").toString()));
            coin.setLastUpdated(LocalDateTime.now());

            coinRepository.save(coin);
            log.debug("Saved coin info for {}", symbol);

        } catch (Exception e) {
            log.error("Error fetching coin info for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Coin info fetch failed", e);
        }
    }

    private void fetchAndSavePriceHistory(String symbol, String interval, int limit) {

        priceHistoryModel obj = priceHistoryRepository.findFirstBySymbolOrderByIdDesc(symbol);
        LocalDateTime lastUpdate = null;
        if (obj != null) {
            lastUpdate = obj.getTimestamp();
        }

        String url = String.format(
                "%s/klines?symbol=%sUSDT&interval=%s&limit=%d",
                BINANCE_API_URL,
                symbol,
                interval,
                limit);
        // Nếu có lastUpdate, thêm tham số startTime để lấy dữ liệu mới
        if (lastUpdate != null) {
            long startTime = lastUpdate.atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
            long endTime = System.currentTimeMillis();
            long newLimit = (endTime - startTime) / 300000;
            System.out.println(startTime + ":" + endTime + ":" + newLimit);
            url = String.format(
                    "%s/klines?symbol=%sUSDT&startTime=%d&interval=%s&limit=%d",
                    BINANCE_API_URL,
                    symbol,
                    startTime,
                    interval,
                    newLimit);

        }

        try {
            ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<List<Object>>>() {
                    });

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Invalid API response for price history");
            }

            List<List<Object>> klines = response.getBody();
            List<priceHistoryModel> histories = new ArrayList<>();

            for (List<Object> kline : klines) {
                LocalDateTime timestamp = Instant.ofEpochMilli(Long.parseLong(kline.get(0).toString()))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                // Bỏ qua nếu timestamp không mới hơn lastUpdate
                if (lastUpdate != null && !timestamp.isAfter(lastUpdate)) {
                    continue;
                }
                priceHistoryModel history = new priceHistoryModel();
                history.setSymbol(symbol);
                history.setOpenPrice(new BigDecimal(kline.get(1).toString()));
                history.setHighPrice(new BigDecimal(kline.get(2).toString()));
                history.setLowPrice(new BigDecimal(kline.get(3).toString()));
                history.setClosePrice(new BigDecimal(kline.get(4).toString()));
                history.setVolume(new BigDecimal(kline.get(5).toString()));
                history.setIntervalType(interval);
                history.setTimestamp(
                        Instant.ofEpochMilli(Long.parseLong(kline.get(0).toString()))
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime());
                histories.add(history);
            }

            if (!histories.isEmpty()) {
                priceHistoryRepository.saveAll(histories);
                // Cập nhật lastUpdateTimestamps với thời gian mới nhất
                log.info("Saved {} new price records for {}", histories.size(), symbol);
            } else {
                log.debug("No new price records for {}", symbol);
            }

        } catch (Exception e) {
            log.error("Error fetching price history for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Price history fetch failed", e);
        }
    }

    // Hàm cập nhật lịch sử giá BTC mỗi 10 giây
    // @Scheduled(fixedRate = 10000)
    public void updateBtcCoinInfo() {
        TRACKED_SYMBOLS.forEach(symbol -> {
            try {
                log.debug("Starting scheduled coin info update for {}", symbol);
                fetchAndSaveCoinInfo(symbol);
                log.debug("Completed scheduled coin info update for {}", symbol);
            } catch (Exception e) {
                log.error("Scheduled coin info update failed for {}: {}", symbol, e.getMessage());
            }
        });
    }

    // @Scheduled(cron = "0 0/5 * * * *")
    public void updatePriceHistory() {
        TRACKED_SYMBOLS.forEach(symbol -> {
            try {
                String url = String.format(
                        "%s/klines?symbol=%sUSDT&interval=%s&limit=%d",
                        BINANCE_API_URL,
                        symbol,
                        "5m",
                        1);
                ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<List<Object>>>() {
                        });

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new RuntimeException("Invalid API response for price history");
                }

                List<List<Object>> klines = response.getBody();
                List<priceHistoryModel> histories = new ArrayList<>();

                for (List<Object> kline : klines) {
                    priceHistoryModel history = new priceHistoryModel();
                    history.setSymbol(symbol);
                    history.setOpenPrice(new BigDecimal(kline.get(1).toString()));
                    history.setHighPrice(new BigDecimal(kline.get(2).toString()));
                    history.setLowPrice(new BigDecimal(kline.get(3).toString()));
                    history.setClosePrice(new BigDecimal(kline.get(4).toString()));
                    history.setVolume(new BigDecimal(kline.get(5).toString()));
                    history.setIntervalType("5m");
                    history.setTimestamp(
                            Instant.ofEpochMilli(Long.parseLong(kline.get(0).toString()))
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime());
                    histories.add(history);
                }

                if (!histories.isEmpty()) {
                    priceHistoryRepository.saveAll(histories);
                    // Cập nhật lastUpdateTimestamps với thời gian mới nhất
                    log.info("Saved {} new price records for {}", histories.size(), symbol);
                } else {
                    log.debug("No new price records for {}", symbol);
                }
            } catch (Exception e) {

            }
        });
    }

    @Cacheable(value = "hisPriceCoin", key = "#entity.name", unless = "#result == null")
    public List<priceHistoryModel> getListHisCoin(coinModel entity) {
        return priceHistoryRepository.findTop288BySymbolOrderByTimestampDesc(entity.getName());
    }
}