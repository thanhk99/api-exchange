package api.exchange.controllers;

import api.exchange.dtos.Request.FuturesKlineRequest;
import api.exchange.dtos.Response.KlinesFuturesResponse;
import api.exchange.services.FuturesKlineCalculationService;
import api.exchange.services.FuturesMarketDataService;
import api.exchange.services.FuturesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/public/futures")
@CrossOrigin(origins = "*")
public class PublicFuturesController {

    @Autowired
    private FuturesMarketDataService futuresMarketDataService;

    @Autowired
    private FuturesOrderService futuresOrderService;

    @Autowired
    private FuturesKlineCalculationService futuresKlineCalculationService;

    @GetMapping("/coins")
    public ResponseEntity<?> getFuturesCoins() {
        try {
            // Get Futures-specific market data (Mark Price, Funding Rate, etc.)
            var futuresMarkets = futuresMarketDataService.getAllFuturesMarkets();
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", futuresMarkets));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<?> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            var orderBook = futuresOrderService.getOrderBook(symbol, limit);
            return ResponseEntity.ok(Map.of(
                    "message", "Success",
                    "data", orderBook));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- Kline Data ---

    @PostMapping("/kline")
    public ResponseEntity<Map<String, Object>> getKlineData(@RequestBody FuturesKlineRequest request,
            @RequestParam(defaultValue = "72") int limit,
            @RequestParam(required = false) Long endTime) {

        try {
            List<KlinesFuturesResponse> klineData = getKlineDataByInterval(request.getSymbol(), request.getInterval(),
                    limit, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.getSymbol().toUpperCase());
            response.put("interval", request.getInterval());
            response.put("data", klineData);
            response.put("count", klineData.size());
            response.put("limit", limit);
            response.put("success", true);
            response.put("message", "Dữ liệu futures kline được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy dữ liệu futures kline: " + e.getMessage());
            errorResponse.put("symbol", request.getSymbol().toUpperCase());
            errorResponse.put("interval", request.getInterval());
            errorResponse.put("data", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private List<KlinesFuturesResponse> getKlineDataByInterval(String symbol, String interval, int limit,
            Long endTime) {
        switch (interval.toLowerCase()) {
            case "1s":
                return futuresKlineCalculationService.get1sKlines(symbol, limit, endTime);
            case "1m":
                return futuresKlineCalculationService.get1mKlines(symbol, limit, endTime);
            case "5m":
                return futuresKlineCalculationService.calculate5mKlines(symbol, limit, endTime);
            case "15m":
                return futuresKlineCalculationService.calculate15mKlines(symbol, limit, endTime);
            case "1h":
                return futuresKlineCalculationService.get1hKlines(symbol, limit, endTime);
            case "6h":
                return futuresKlineCalculationService.calculate6hKlines(symbol, limit, endTime);
            case "12h":
                return futuresKlineCalculationService.calculate12hKlines(symbol, limit, endTime);
            default:
                throw new IllegalArgumentException("Khoảng thời gian không được hỗ trợ: " + interval);
        }
    }

    @GetMapping("/kline/intervals")
    public ResponseEntity<Map<String, Object>> getSupportedIntervals() {
        try {
            String[] intervals = { "1s", "1m", "5m", "15m", "1h", "6h", "12h" };

            Map<String, Object> response = new HashMap<>();
            response.put("intervals", intervals);
            response.put("count", intervals.length);
            response.put("success", true);
            response.put("message", "Danh sách khoảng thời gian được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/kline/symbols")
    public ResponseEntity<Map<String, Object>> getSupportedSymbols() {
        try {
            String[] symbols = { "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
                    "ADAUSDT", "DOGEUSDT", "TRXUSDT", "DOTUSDT", "LTCUSDT" };

            Map<String, Object> response = new HashMap<>();
            response.put("symbols", symbols);
            response.put("count", symbols.length);
            response.put("success", true);
            response.put("message", "Danh sách symbols được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/kline/{symbol}/{interval}/info")
    public ResponseEntity<Map<String, Object>> getKlineInfo(
            @PathVariable String symbol,
            @PathVariable String interval) {

        try {
            List<KlinesFuturesResponse> klineData = getKlineDataByInterval(symbol, interval, 1, null);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol.toUpperCase());
            response.put("interval", interval);
            response.put("hasData", !klineData.isEmpty());
            response.put("dataCount", klineData.size());

            if (!klineData.isEmpty()) {
                KlinesFuturesResponse latest = klineData.get(0);
                response.put("latestKline", latest);
                response.put("lastUpdateTime", latest.getCloseTime());
            }

            response.put("success", true);
            response.put("message", "Thông tin futures kline được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/kline/all/{interval}")
    public ResponseEntity<Map<String, Object>> getAllSymbolsKlineData(
            @PathVariable String interval,
            @RequestParam(defaultValue = "72") int limit) {

        try {
            String[] symbols = { "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT" };
            Map<String, List<KlinesFuturesResponse>> allData = new HashMap<>();

            for (String symbol : symbols) {
                List<KlinesFuturesResponse> klineData = getKlineDataByInterval(symbol, interval, limit, null);
                allData.put(symbol, klineData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("interval", interval);
            response.put("data", allData);
            response.put("symbols", symbols);
            response.put("limit", limit);
            response.put("success", true);
            response.put("message", "Dữ liệu futures kline cho tất cả symbols được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
