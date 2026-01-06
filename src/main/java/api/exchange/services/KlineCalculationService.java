package api.exchange.services;

import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.models.SpotKlineData1m;
import api.exchange.models.SpotKlineData1h;
import api.exchange.repository.SpotKlineData1mRepository;
import api.exchange.repository.SpotKlineData1hRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class KlineCalculationService {

    @Autowired
    private SpotKlineData1mRepository spotKlineData1mRepository;

    @Autowired
    private SpotKlineData1hRepository spotKlineData1hRepository;

    @Autowired
    private api.exchange.repository.SpotKlineData1sRepository spotKlineData1sRepository;

    /**
     * Lấy dữ liệu kline 1s từ database
     */
    public List<KlinesSpotResponse> get1sKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();
        try {
            List<api.exchange.models.SpotKlineData1s> klines1s = spotKlineData1sRepository.findLatestKlines(symbol,
                    limit);
            for (api.exchange.models.SpotKlineData1s kline : klines1s) {
                KlinesSpotResponse response = new KlinesSpotResponse(
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
        } catch (Exception e) {
            System.err.println("❌ Error getting 1s klines for " + symbol + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Tính toán dữ liệu kline 5m từ dữ liệu 1m
     */
    public List<KlinesSpotResponse> calculate5mKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1m gần nhất
            List<SpotKlineData1m> klines1m = spotKlineData1mRepository.findLatest72Klines(symbol);

            if (klines1m.size() < 5) {
                return result;
            }

            // Tính toán các nến 5m
            for (int i = 0; i < klines1m.size() - 4; i += 5) {
                if (i + 4 < klines1m.size()) {
                    List<SpotKlineData1m> fiveMinuteKlines = klines1m.subList(i, i + 5);
                    KlinesSpotResponse calculatedKline = calculateKlineFrom1mData(fiveMinuteKlines, "5m");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating 5m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 15m từ dữ liệu 1m
     */
    public List<KlinesSpotResponse> calculate15mKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1m gần nhất
            List<SpotKlineData1m> klines1m = spotKlineData1mRepository.findLatest72Klines(symbol);

            if (klines1m.size() < 15) {
                return result;
            }

            // Tính toán các nến 15m
            for (int i = 0; i < klines1m.size() - 14; i += 15) {
                if (i + 14 < klines1m.size()) {
                    List<SpotKlineData1m> fifteenMinuteKlines = klines1m.subList(i, i + 15);
                    KlinesSpotResponse calculatedKline = calculateKlineFrom1mData(fifteenMinuteKlines, "15m");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating 15m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 6h từ dữ liệu 1h
     */
    public List<KlinesSpotResponse> calculate6hKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1h gần nhất
            List<SpotKlineData1h> klines1h = spotKlineData1hRepository.findLatest72Klines(symbol);

            if (klines1h.size() < 6) {
                return result;
            }

            // Tính toán các nến 6h
            for (int i = 0; i < klines1h.size() - 5; i += 6) {
                if (i + 5 < klines1h.size()) {
                    List<SpotKlineData1h> sixHourKlines = klines1h.subList(i, i + 6);
                    KlinesSpotResponse calculatedKline = calculateKlineFrom1hData(sixHourKlines, "6h");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating 6h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán dữ liệu kline 12h từ dữ liệu 1h
     */
    public List<KlinesSpotResponse> calculate12hKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            // Lấy dữ liệu 1h gần nhất
            List<SpotKlineData1h> klines1h = spotKlineData1hRepository.findLatest72Klines(symbol);

            if (klines1h.size() < 12) {
                return result;
            }

            // Tính toán các nến 12h
            for (int i = 0; i < klines1h.size() - 11; i += 12) {
                if (i + 11 < klines1h.size()) {
                    List<SpotKlineData1h> twelveHourKlines = klines1h.subList(i, i + 12);
                    KlinesSpotResponse calculatedKline = calculateKlineFrom1hData(twelveHourKlines, "12h");
                    result.add(calculatedKline);

                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error calculating 12h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính toán nến kline từ dữ liệu 1m
     */
    private KlinesSpotResponse calculateKlineFrom1mData(List<SpotKlineData1m> klines1m, String interval) {
        if (klines1m.isEmpty()) {
            return null;
        }

        SpotKlineData1m first = klines1m.get(0);
        SpotKlineData1m last = klines1m.get(klines1m.size() - 1);

        BigDecimal openPrice = first.getOpenPrice();
        BigDecimal closePrice = last.getClosePrice();
        BigDecimal highPrice = klines1m.stream()
                .map(SpotKlineData1m::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal lowPrice = klines1m.stream()
                .map(SpotKlineData1m::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = klines1m.stream()
                .map(SpotKlineData1m::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long startTime = first.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long closeTime = last.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return new KlinesSpotResponse(
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
    private KlinesSpotResponse calculateKlineFrom1hData(List<SpotKlineData1h> klines1h, String interval) {
        if (klines1h.isEmpty()) {
            return null;
        }

        SpotKlineData1h first = klines1h.get(0);
        SpotKlineData1h last = klines1h.get(klines1h.size() - 1);

        BigDecimal openPrice = first.getOpenPrice();
        BigDecimal closePrice = last.getClosePrice();
        BigDecimal highPrice = klines1h.stream()
                .map(SpotKlineData1h::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal lowPrice = klines1h.stream()
                .map(SpotKlineData1h::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = klines1h.stream()
                .map(SpotKlineData1h::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long startTime = first.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long closeTime = last.getCloseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return new KlinesSpotResponse(
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
    public List<KlinesSpotResponse> get1mKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            List<SpotKlineData1m> klines1m = spotKlineData1mRepository.findLatest72Klines(symbol);

            for (SpotKlineData1m kline : klines1m) {
                if (result.size() >= limit)
                    break;

                KlinesSpotResponse response = new KlinesSpotResponse(
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
            System.err.println("❌ Error getting 1m klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Lấy dữ liệu kline 1h trực tiếp từ database
     */
    public List<KlinesSpotResponse> get1hKlines(String symbol, int limit) {
        List<KlinesSpotResponse> result = new ArrayList<>();

        try {
            List<SpotKlineData1h> klines1h = spotKlineData1hRepository.findLatest72Klines(symbol);

            for (SpotKlineData1h kline : klines1h) {
                if (result.size() >= limit)
                    break;

                KlinesSpotResponse response = new KlinesSpotResponse(
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
            System.err.println("❌ Error getting 1h klines for " + symbol + ": " + e.getMessage());
        }

        return result;
    }
}
