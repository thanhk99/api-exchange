package api.exchange.controllers;

import api.exchange.dtos.Request.FuturesOrderRequest;
import api.exchange.models.FuturesOrder;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.services.FuturesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/futures/orders")
public class FuturesOrderController {

    @Autowired
    private FuturesOrderService futuresOrderService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractUid(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
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

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            List<FuturesOrder> orders = futuresOrderService.getOrders(uid, symbol, status, limit, offset);
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", orders,
                    "total", orders.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FuturesOrderRequest request) {
        try {
            String uid = extractUid(authHeader);
            FuturesOrder order = futuresOrderService.placeOrder(
                    uid,
                    request.getSymbol(),
                    request.getSide(),
                    request.getPositionSide(),
                    request.getType(),
                    request.getPrice(),
                    request.getQuantity(),
                    request.getLeverage());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        try {
            String uid = extractUid(authHeader);
            futuresOrderService.cancelOrder(uid, orderId);
            return ResponseEntity.ok(Map.of(
                    "message", "Order cancelled successfully",
                    "orderId", orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
