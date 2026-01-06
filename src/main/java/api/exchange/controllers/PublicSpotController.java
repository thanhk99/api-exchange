package api.exchange.controllers;

import api.exchange.dtos.Request.SpotKlineRequest;
import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.models.coinModel;
import api.exchange.services.KlineCalculationService;
import api.exchange.services.RingBufferService;
import api.exchange.services.SpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/spot")
@CrossOrigin(origins = "*")
public class PublicSpotController {

    @Autowired
    private KlineCalculationService klineCalculationService;

    @Autowired
    private RingBufferService ringBufferService;

    @Autowired
    private SpotService spotService;

    // --- Order Book ---

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<?> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            var orderBook = spotService.getOrderBook(symbol, limit);
            return ResponseEntity.ok(orderBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- Standard Kline Data ---

    @PostMapping("/kline")
    public ResponseEntity<Map<String, Object>> getKlineData(@RequestBody SpotKlineRequest request,
            @RequestParam(defaultValue = "500") int limit) {

        try {
            List<KlinesSpotResponse> klineData = getKlineDataByInterval(request.getSymbol(), request.getInterval(),
                    limit);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.getSymbol().toUpperCase());
            response.put("interval", request.getInterval());
            response.put("data", klineData);
            response.put("count", klineData.size());
            response.put("limit", limit);
            response.put("success", true);
            response.put("message", "Dữ liệu kline được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy dữ liệu kline: " + e.getMessage());
            errorResponse.put("symbol", request.getSymbol().toUpperCase());
            errorResponse.put("interval", request.getInterval());
            errorResponse.put("data", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private List<KlinesSpotResponse> getKlineDataByInterval(String symbol, String interval, int limit) {
        switch (interval.toLowerCase()) {
            case "1s":
                return klineCalculationService.get1sKlines(symbol, limit);
            case "1m":
                return klineCalculationService.get1mKlines(symbol, limit);
            case "5m":
                return klineCalculationService.calculate5mKlines(symbol, limit);
            case "15m":
                return klineCalculationService.calculate15mKlines(symbol, limit);
            case "1h":
                return klineCalculationService.get1hKlines(symbol, limit);
            case "6h":
                return klineCalculationService.calculate6hKlines(symbol, limit);
            case "12h":
                return klineCalculationService.calculate12hKlines(symbol, limit);
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
            String[] symbols = { "BTCUSDT", "ETHUSDT", "SOLUSDT" }; // Should fetch from dynamic source ideally

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

    // --- Realtime Kline Data (RingBuffer) ---

    @PostMapping("/kline/realtime")
    public ResponseEntity<?> getKlineRealtime(@RequestBody coinModel request) {
        try {
            List<KlinesSpotResponse> klineRealtime = ringBufferService.getKlineData(request.getSymbol());
            int currentSize = ringBufferService.getCurrentSize(request.getSymbol());

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.getSymbol().toUpperCase());
            response.put("data", klineRealtime);
            response.put("count", currentSize);
            response.put("maxSize", 72);
            response.put("success", true);
            response.put("message", "Dữ liệu kline được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy dữ liệu kline: " + e.getMessage());
            errorResponse.put("symbol", request.getSymbol().toUpperCase());
            errorResponse.put("data", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
