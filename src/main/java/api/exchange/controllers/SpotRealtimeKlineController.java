package api.exchange.controllers;

import api.exchange.dtos.Response.KlinesSpotResponse;
import api.exchange.models.coinModel;
import api.exchange.services.RingBufferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/kline1s")
@CrossOrigin(origins = "*")
public class SpotRealtimeKlineController {

    @Autowired
    private RingBufferService ringBufferService;

    @PostMapping("/symbol")
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

    @GetMapping("/symbols")
    public ResponseEntity<Map<String, Object>> getAllSymbols() {
        try {
            Set<String> symbols = ringBufferService.getAllSymbols();

            Map<String, Object> response = new HashMap<>();
            response.put("symbols", symbols);
            response.put("count", symbols.size());
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
     * Lấy thông tin chi tiết về buffer của một symbol
     * 
     * @param symbol Symbol cần kiểm tra
     * @return Thông tin chi tiết về buffer
     */
    @GetMapping("/{symbol}/info")
    public ResponseEntity<Map<String, Object>> getSymbolInfo(@PathVariable String symbol) {
        try {
            int currentSize = ringBufferService.getCurrentSize(symbol);
            List<KlinesSpotResponse> klineData = ringBufferService.getKlineData(symbol);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol.toUpperCase());
            response.put("currentSize", currentSize);
            response.put("maxSize", 72);
            response.put("isFull", currentSize == 72);
            response.put("isEmpty", currentSize == 0);
            response.put("hasData", currentSize > 0);

            if (currentSize > 0 && !klineData.isEmpty()) {
                KlinesSpotResponse latest = klineData.get(klineData.size() - 1);
                KlinesSpotResponse oldest = klineData.get(0);

                response.put("latestKline", latest);
                response.put("oldestKline", oldest);
                response.put("timeRange", Map.of(
                        "startTime", oldest.getStartTime(),
                        "endTime", latest.getCloseTime()));
            }

            response.put("success", true);
            response.put("message", "Thông tin symbol được lấy thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thông tin symbol: " + e.getMessage());
            errorResponse.put("symbol", symbol.toUpperCase());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Xóa dữ liệu của một symbol cụ thể
     * 
     * @param symbol Symbol cần xóa dữ liệu
     * @return Kết quả xóa dữ liệu
     */
    @DeleteMapping("/{symbol}")
    public ResponseEntity<Map<String, Object>> clearSymbolData(@PathVariable String symbol) {
        try {
            int sizeBefore = ringBufferService.getCurrentSize(symbol);
            ringBufferService.clearSymbolData(symbol);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol.toUpperCase());
            response.put("clearedCount", sizeBefore);
            response.put("success", true);
            response.put("message", "Dữ liệu của symbol đã được xóa thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi xóa dữ liệu symbol: " + e.getMessage());
            errorResponse.put("symbol", symbol.toUpperCase());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
