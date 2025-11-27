package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.P2PAd;
import api.exchange.models.P2PAd.TradeType;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.services.P2PADService;
import api.exchange.services.P2POrderService;
import api.exchange.dtos.Request.OrderRequest;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/p2pads")
public class P2PADController {
    @Autowired
    private P2PADService p2padService;
    @Autowired
    private P2POrderService p2pOrderService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("create")
    public ResponseEntity<?> createAds(@RequestBody P2PAd p2pAd, @RequestHeader("Authorization") String authHeader) {
        return p2padService.createAddP2P(p2pAd, authHeader);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAd(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        return p2padService.cancelAd(id, authHeader);
    }

    @PostMapping("order")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.createOrder(request.getAdId(), request.getAmount(), authHeader);
    }

    @PostMapping("order/{orderId}/confirm")
    public ResponseEntity<?> buyerConfirmPayment(@PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.buyerConfirmPayment(orderId, authHeader);
    }

    @PostMapping("order/{orderId}/release")
    public ResponseEntity<?> sellerConfirmPayment(@PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.sellerConfirmPaymentReceived(orderId, authHeader);
    }

    @PostMapping("order/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.cancelOrder(orderId, authHeader);
    }

    @GetMapping("order/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.getOrderDetail(orderId, authHeader);
    }

    @GetMapping("getList/{type}")
    public ResponseEntity<?> getListP2PAds(@PathVariable TradeType type) {
        return p2padService.getListP2PAds(type);
    }

    /**
     * Endpoint để "Mở một tranh chấp (Khiếu nại)".
     * Cả người mua và người bán đều có thể gọi endpoint này.
     *
     * @param transactionId ID của giao dịch cần khiếu nại.
     * @param authHeader    JWT token của người dùng đang thực hiện.
     * @return ResponseEntity với trạng thái tranh chấp của giao dịch.
     */
    @PostMapping("/{id}/dispute")
    public ResponseEntity<?> openDispute(
            @PathVariable("id") Long transactionId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        return p2padService.openDispute(transactionId, userId);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfileP2P(@RequestHeader("Authorization") String header) {
        return p2padService.profileUserP2P(header);
    }

    @GetMapping("/myads")
    public ResponseEntity<?> getMyAds(@RequestHeader("Authorization") String authHeader) {
        return p2padService.getUserAds(authHeader);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.getUserProfile(authHeader);
    }

    @GetMapping("/user/history")
    public ResponseEntity<?> getUserHistory(@RequestHeader("Authorization") String authHeader) {
        return p2pOrderService.getUserTransactionHistory(authHeader);
    }

}
