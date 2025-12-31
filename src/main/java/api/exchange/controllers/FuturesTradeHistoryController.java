package api.exchange.controllers;

import api.exchange.models.FuturesTransaction;
import api.exchange.repository.FuturesTransactionRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/futures/trades")
@CrossOrigin(origins = "*")
public class FuturesTradeHistoryController {

    @Autowired
    private FuturesTransactionRepository futuresTransactionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Lấy lịch sử giao dịch của user (cần authentication)
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyTradeHistory(
            @RequestHeader("Authorization") String header,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            String jwt = header.substring(7);
            String uid = jwtUtil.getUserIdFromToken(jwt);

            Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("createdAt").descending());
            List<FuturesTransaction> trades;

            if (type != null && !type.isEmpty()) {
                FuturesTransaction.TransactionType transactionType = FuturesTransaction.TransactionType
                        .valueOf(type.toUpperCase());
                trades = futuresTransactionRepository.findByUidAndType(uid, transactionType, pageable);
            } else {
                trades = futuresTransactionRepository.findByUid(uid, pageable);
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
            return futuresTransactionRepository.findById(tradeId)
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
