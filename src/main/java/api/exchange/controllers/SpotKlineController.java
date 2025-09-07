package api.exchange.controllers;

import api.exchange.dtos.Requset.SpotKlineRequest;
import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.services.KlineCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spotKline")
@CrossOrigin(origins = "*")
public class SpotKlineController {

    @Autowired
    private KlineCalculationService klineCalculationService;

    /**
     * Lấy dữ liệu kline của một symbol với khoảng thời gian cụ thể
     * 
     * @param symbol   Symbol cần lấy dữ liệu (ví dụ: BTCUSDT)
     * @param interval Khoảng thời gian (1m, 5m, 15m, 1h, 6h, 12h)
     * @param limit    Số lượng nến cần lấy (mặc định 72)
     * @return Danh sách dữ liệu kline
     */
    @PostMapping("/symbol")
    public ResponseEntity<Map<String, Object>> getKlineData(@RequestBody SpotKlineRequest request,
            @RequestParam(defaultValue = "72") int limit) {

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

    /**
     * Lấy dữ liệu kline theo khoảng thời gian
     */
    private List<KlinesSpotResponse> getKlineDataByInterval(String symbol, String interval, int limit) {
        switch (interval.toLowerCase()) {
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

    /**
     * Lấy danh sách các khoảng thời gian được hỗ trợ
     */
    @GetMapping("/intervals")
    public ResponseEntity<Map<String, Object>> getSupportedIntervals() {
        try {
            String[] intervals = { "1m", "5m", "15m", "1h", "6h", "12h" };

            Map<String, Object> response = new HashMap<>();
            response.put("intervals", intervals);
            response.put("count", intervals.length);
            response.put("success", true);
            response.put("message", "Danh sách khoảng thời gian được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách khoảng thời gian: " + e.getMessage());
            errorResponse.put("intervals", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Lấy danh sách symbols được hỗ trợ
     */
    @GetMapping("/symbols")
    public ResponseEntity<Map<String, Object>> getSupportedSymbols() {
        try {
            String[] symbols = { "BTCUSDT", "ETHUSDT", "SOLUSDT" };

            Map<String, Object> response = new HashMap<>();
            response.put("symbols", symbols);
            response.put("count", symbols.length);
            response.put("success", true);
            response.put("message", "Danh sách symbols được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách symbols: " + e.getMessage());
            errorResponse.put("symbols", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Lấy thông tin chi tiết về dữ liệu của một symbol và interval
     */
    @GetMapping("/{symbol}/{interval}/info")
    public ResponseEntity<Map<String, Object>> getKlineInfo(
            @PathVariable String symbol,
            @PathVariable String interval) {

        try {
            List<KlinesSpotResponse> klineData = getKlineDataByInterval(symbol, interval, 1);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol.toUpperCase());
            response.put("interval", interval);
            response.put("hasData", !klineData.isEmpty());
            response.put("dataCount", klineData.size());

            if (!klineData.isEmpty()) {
                KlinesSpotResponse latest = klineData.get(0);
                response.put("latestKline", latest);
                response.put("lastUpdateTime", latest.getCloseTime());
            }

            response.put("success", true);
            response.put("message", "Thông tin kline được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thông tin kline: " + e.getMessage());
            errorResponse.put("symbol", symbol.toUpperCase());
            errorResponse.put("interval", interval);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Lấy dữ liệu kline cho tất cả symbols với một khoảng thời gian
     */
    @GetMapping("/all/{interval}")
    public ResponseEntity<Map<String, Object>> getAllSymbolsKlineData(
            @PathVariable String interval,
            @RequestParam(defaultValue = "72") int limit) {

        try {
            String[] symbols = { "BTCUSDT", "ETHUSDT", "SOLUSDT" };
            Map<String, List<KlinesSpotResponse>> allData = new HashMap<>();

            for (String symbol : symbols) {
                List<KlinesSpotResponse> klineData = getKlineDataByInterval(symbol, interval, limit);
                allData.put(symbol, klineData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("interval", interval);
            response.put("data", allData);
            response.put("symbols", symbols);
            response.put("limit", limit);
            response.put("success", true);
            response.put("message", "Dữ liệu kline cho tất cả symbols được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy dữ liệu kline cho tất cả symbols: " + e.getMessage());
            errorResponse.put("interval", interval);
            errorResponse.put("data", null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
