package api.exchange.services;

import api.exchange.dtos.Response.KlinesFuturesResponse;
import api.exchange.models.FuturesKlineData1m;
import api.exchange.models.FuturesKlineData1h;
import api.exchange.repository.FuturesKlineData1mRepository;
import api.exchange.repository.FuturesKlineData1hRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class FuturesKlineCalculationService {

    @Autowired
    private FuturesKlineData1mRepository futuresKlineData1mRepository;

    @Autowired
    private FuturesKlineData1hRepository futuresKlineData1hRepository;

    @Autowired
    private api.exchange.repository.FuturesKlineData1sRepository futuresKlineData1sRepository;

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * Lấy dữ liệu kline 1s từ Redis (nếu có) hoặc database
     */
    public List<KlinesFuturesResponse> get1sKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            // 1. Thử lấy từ Cache
            List<KlinesFuturesResponse> cachedKlines = redisCacheService.getCached1sKlines(symbol);
            if (cachedKlines != null && !cachedKlines.isEmpty()) {
                // Nếu limit yêu cầu nhỏ hơn cache, trả về sublist
                if (cachedKlines.size() >= limit) {
                    return cachedKlines.subList(0, limit);
                }
                // Nếu cache không đủ (ít khi xảy ra nếu logic cache đúng), fall back xuống DB
            }

            // 2. Nếu không có trong cache, lấy từ DB
            List<api.exchange.models.FuturesKlineData1s> klines1s;
            if (endTime != null && endTime > 0) {
                // Convert timestamp to LocalDateTime if needed or just query directly if
                // repository supports it.
                // Assuming 1s repo doesn't have the method yet, falling back to latest for now
                // or we should add it.
                // For this task, we focus on 1m and 1h as per plan, but let's keep consistent
                // signature
                klines1s = futuresKlineData1sRepository.findLatestKlines(symbol, limit);
            } else {
                klines1s = futuresKlineData1sRepository.findLatestKlines(symbol, limit);
            }

            for (api.exchange.models.FuturesKlineData1s kline : klines1s) {
                if (result.size() >= limit)
                    break;

                KlinesFuturesResponse response = new KlinesFuturesResponse(
                        kline.getSymbol(),
                        kline.getOpenPrice(),
                        kline.getClosePrice(),
                        kline.getHighPrice(),
                        kline.getLowPrice(),
                        kline.getVolume(),
                        kline.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        kline.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        "1s",
                        kline.getIsClosed());
                result.add(response);
            }

            // 3. Lưu vào Cache (nếu có dữ liệu)
            if (!result.isEmpty()) {
                redisCacheService.cache1sKlines(symbol, result);
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting futures 1s klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 5m từ dữ liệu 1m
     */
    public List<KlinesFuturesResponse> calculate5mKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1m gần nhất (cần limit * 5 nến 1m để tạo limit nến 5m)
            int requiredKlines = Math.min(limit * 5, 500); // Giới hạn tối đa 500 klines
            List<FuturesKlineData1m> klines1m;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1m = futuresKlineData1mRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, requiredKlines);
            } else {
                klines1m = futuresKlineData1mRepository.findLatestKlines(symbol, requiredKlines);
            }

            if (klines1m.size() < 5) {
                return result;
            }

            // Tính toán các nến 5m
            for (int i = 0; i < klines1m.size() - 4; i += 5) {
                if (i + 4 < klines1m.size()) {
                    List<FuturesKlineData1m> fiveMinuteKlines = klines1m.subList(i, i + 5);
                    KlinesFuturesResponse calculatedKline = calculateKlineFrom1mData(fiveMinuteKlines, "5m");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating futures 5m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 15m từ dữ liệu 1m
     */
    public List<KlinesFuturesResponse> calculate15mKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1m gần nhất (cần limit * 15 nến 1m để tạo limit nến 15m)
            int requiredKlines = Math.min(limit * 15, 500);
            List<FuturesKlineData1m> klines1m;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1m = futuresKlineData1mRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, requiredKlines);
            } else {
                klines1m = futuresKlineData1mRepository.findLatestKlines(symbol, requiredKlines);
            }

            if (klines1m.size() < 15) {
                return result;
            }

            // Tính toán các nến 15m
            for (int i = 0; i < klines1m.size() - 14; i += 15) {
                if (i + 14 < klines1m.size()) {
                    List<FuturesKlineData1m> fifteenMinuteKlines = klines1m.subList(i, i + 15);
                    KlinesFuturesResponse calculatedKline = calculateKlineFrom1mData(fifteenMinuteKlines, "15m");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating futures 15m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 6h từ dữ liệu 1h
     */
    public List<KlinesFuturesResponse> calculate6hKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1h gần nhất (cần limit * 6 nến 1h để tạo limit nến 6h)
            int requiredKlines = Math.min(limit * 6, 500);
            List<FuturesKlineData1h> klines1h;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1h = futuresKlineData1hRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, requiredKlines);
            } else {
                klines1h = futuresKlineData1hRepository.findLatestKlines(symbol, requiredKlines);
            }

            if (klines1h.size() < 6) {
                return result;
            }

            // Tính toán các nến 6h
            for (int i = 0; i < klines1h.size() - 5; i += 6) {
                if (i + 5 < klines1h.size()) {
                    List<FuturesKlineData1h> sixHourKlines = klines1h.subList(i, i + 6);
                    KlinesFuturesResponse calculatedKline = calculateKlineFrom1hData(sixHourKlines, "6h");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating futures 6h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 12h từ dữ liệu 1h
     */
    public List<KlinesFuturesResponse> calculate12hKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1h gần nhất (cần limit * 12 nến 1h để tạo limit nến 12h)
            int requiredKlines = Math.min(limit * 12, 500);
            List<FuturesKlineData1h> klines1h;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1h = futuresKlineData1hRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, requiredKlines);
            } else {
                klines1h = futuresKlineData1hRepository.findLatestKlines(symbol, requiredKlines);
            }

            if (klines1h.size() < 12) {
                return result;
            }

            // Tính toán các nến 12h
            for (int i = 0; i < klines1h.size() - 11; i += 12) {
                if (i + 11 < klines1h.size()) {
                    List<FuturesKlineData1h> twelveHourKlines = klines1h.subList(i, i + 12);
                    KlinesFuturesResponse calculatedKline = calculateKlineFrom1hData(twelveHourKlines, "12h");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating futures 12h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán nến kline từ dữ liệu 1m
     */
    private KlinesFuturesResponse calculateKlineFrom1mData(List<FuturesKlineData1m> klines1m, String interval) {
        if (klines1m.isEmpty()) {
            return null;
        }

        FuturesKlineData1m first = klines1m.get(0);
        FuturesKlineData1m last = klines1m.get(klines1m.size() - 1);

        BigDecimal openPrice = first.getOpenPrice();
        BigDecimal closePrice = last.getClosePrice();
        BigDecimal highPrice = klines1m.stream()
                .map(FuturesKlineData1m::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal lowPrice = klines1m.stream()
                .map(FuturesKlineData1m::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = klines1m.stream()
                .map(FuturesKlineData1m::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long startTime = first.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long closeTime = last.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return new KlinesFuturesResponse(
                first.getSymbol(),
                openPrice,
                closePrice,
                highPrice,
                lowPrice,
                volume,
                startTime,
                closeTime,
                interval,
                true);
    }

    /**
     * Tính toán nến kline từ dữ liệu 1h
     */
    private KlinesFuturesResponse calculateKlineFrom1hData(List<FuturesKlineData1h> klines1h, String interval) {
        if (klines1h.isEmpty()) {
            return null;
        }

        FuturesKlineData1h first = klines1h.get(0);
        FuturesKlineData1h last = klines1h.get(klines1h.size() - 1);

        BigDecimal openPrice = first.getOpenPrice();
        BigDecimal closePrice = last.getClosePrice();
        BigDecimal highPrice = klines1h.stream()
                .map(FuturesKlineData1h::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal lowPrice = klines1h.stream()
                .map(FuturesKlineData1h::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = klines1h.stream()
                .map(FuturesKlineData1h::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long startTime = first.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long closeTime = last.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return new KlinesFuturesResponse(
                first.getSymbol(),
                openPrice,
                closePrice,
                highPrice,
                lowPrice,
                volume,
                startTime,
                closeTime,
                interval,
                true);
    }

    /**
     * Lấy dữ liệu kline 1m trực tiếp từ database
     */
    public List<KlinesFuturesResponse> get1mKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            List<FuturesKlineData1m> klines1m;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1m = futuresKlineData1mRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, limit);
            } else {
                klines1m = futuresKlineData1mRepository.findLatestKlines(symbol, limit);
            }

            for (FuturesKlineData1m kline : klines1m) {
                if (result.size() >= limit)
                    break;

                KlinesFuturesResponse response = new KlinesFuturesResponse(
                        kline.getSymbol(),
                        kline.getOpenPrice(),
                        kline.getClosePrice(),
                        kline.getHighPrice(),
                        kline.getLowPrice(),
                        kline.getVolume(),
                        kline.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        kline.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        "1m",
                        kline.getIsClosed());
                result.add(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting futures 1m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Lấy dữ liệu kline 1h trực tiếp từ database
     */
    public List<KlinesFuturesResponse> get1hKlines(String symbol, int limit, Long endTime) {
        List<KlinesFuturesResponse> result = new ArrayList<>();

        try {
            List<FuturesKlineData1h> klines1h;
            if (endTime != null && endTime > 0) {
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(endTime), java.time.ZoneId.systemDefault());
                klines1h = futuresKlineData1hRepository.findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(symbol,
                        endDateTime, limit);
            } else {
                klines1h = futuresKlineData1hRepository.findLatestKlines(symbol, limit);
            }

            for (FuturesKlineData1h kline : klines1h) {
                if (result.size() >= limit)
                    break;

                KlinesFuturesResponse response = new KlinesFuturesResponse(
                        kline.getSymbol(),
                        kline.getOpenPrice(),
                        kline.getClosePrice(),
                        kline.getHighPrice(),
                        kline.getLowPrice(),
                        kline.getVolume(),
                        kline.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        kline.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        "1h",
                        kline.getIsClosed());
                result.add(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting futures 1h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }
}
