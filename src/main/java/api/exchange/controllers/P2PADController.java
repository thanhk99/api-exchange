package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.P2PAd;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.services.P2PADService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/p2pads")
public class P2PADController {
    @Autowired
    private P2PADService p2padService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("create")
    public ResponseEntity<?> createAds(@RequestBody P2PAd p2pAd, @RequestHeader("Authorization") String authHeader) {
        return p2padService.createAddP2P(p2pAd, authHeader);
    }

    @GetMapping("getList")
    public ResponseEntity<?> getListP2PAds() {
        return p2padService.getListP2PAds();
    }
    // Endpoint cho "Người mua xác nhận đã thanh toán".
    @PostMapping("/{id}/confirm-payment") 
    public ResponseEntity<?> confirmPayment(
            @PathVariable("id") Long transactionId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String buyerId = jwtUtil.getUserIdFromToken(token);

        return p2padService.confirmPayment(transactionId, buyerId);
    }

    /**
     * Endpoint cho "Người bán xác nhận đã nhận tiền" và "Giải phóng coin".
     *
     * @param transactionId ID của giao dịch.
     * @param authHeader    JWT token của người bán.
     * @return ResponseEntity với trạng thái hoàn tất của giao dịch.
     */
    @PostMapping("/{id}/release")
    public ResponseEntity<?> releaseCoins(
            @PathVariable("id") Long transactionId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        String sellerId = jwtUtil.getUserIdFromToken(token);

        return p2padService.releaseCoins(transactionId, sellerId);
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


}
