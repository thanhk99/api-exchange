package api.exchange.controllers;

import api.exchange.models.TransactionSpot;
import api.exchange.repository.TransactionSpotRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spot/trades")
@CrossOrigin(origins = "*")
public class SpotTradeHistoryController {

    @Autowired
    private TransactionSpotRepository transactionSpotRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Lấy lịch sử giao dịch công khai (public trade feed)
     */
    @GetMapping("/history")
    public ResponseEntity<?> getPublicTradeHistory(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("executedAt").descending());
            List<TransactionSpot> trades = transactionSpotRepository.findBySymbol(symbol, pageable);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", trades,
                    "pagination", Map.of(
                            "limit", limit,
                            "offset", offset,
                            "hasMore", trades.size() == limit)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy lịch sử giao dịch: " + e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử giao dịch của user (cần authentication)
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyTradeHistory(
            @RequestHeader("Authorization") String header,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            String jwt = header.substring(7);
            String uid = jwtUtil.getUserIdFromToken(jwt);

            Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("executedAt").descending());
            List<TransactionSpot> trades;

            if (symbol != null && !symbol.isEmpty()) {
                trades = transactionSpotRepository.findByUserAndSymbol(uid, symbol, pageable);
            } else {
                trades = transactionSpotRepository.findByUser(uid, pageable);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", trades,
                    "pagination", Map.of(
                            "limit", limit,
                            "offset", offset,
                            "hasMore", trades.size() == limit)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy lịch sử giao dịch: " + e.getMessage()));
        }
    }

    /**
     * Lấy chi tiết một giao dịch
     */
    @GetMapping("/history/{tradeId}")
    public ResponseEntity<?> getTradeDetail(@PathVariable Long tradeId) {
        try {
            return transactionSpotRepository.findById(tradeId)
                    .map(trade -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "data", trade)))
                    .orElse(ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Không tìm thấy giao dịch")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết giao dịch: " + e.getMessage()));
        }
    }
}
