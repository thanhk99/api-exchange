package api.exchange.services;

import api.exchange.models.P2PAd;
import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2POrderDetail.P2PTransactionStatus;
import api.exchange.models.FundingWallet;
import api.exchange.repository.P2PAdRepository;
import api.exchange.repository.P2POrderDetailRepository;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
public class P2POrderService {

    @Autowired
    private P2PAdRepository p2pAdRepository;

    @Autowired
    private P2POrderDetailRepository orderRepository;

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Buyer creates an order on an existing ad.
     */
    @Transactional
    public ResponseEntity<?> createOrder(Long adId, BigDecimal amount, String authHeader) {
        String token = authHeader.substring(7);
        String buyerId = jwtUtil.getUserIdFromToken(token);
        P2PAd ad = p2pAdRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found"));
        if (!ad.getIsActive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Ad is not active"));
        }
        if (ad.getAvailableAmount().compareTo(amount) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Insufficient amount in ad"));
        }
        // Reduce available amount on the ad
        ad.setAvailableAmount(ad.getAvailableAmount().subtract(amount));
        p2pAdRepository.save(ad);

        // Lock coins in seller's Funding wallet
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(ad.getUserId(), ad.getAsset());
        if (sellerWallet == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Seller wallet not found"));
        }
        if (sellerWallet.getBalance().compareTo(amount) < 0) {
            // Rollback ad available amount
            ad.setAvailableAmount(ad.getAvailableAmount().add(amount));
            p2pAdRepository.save(ad);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Seller has insufficient balance"));
        }
        // Deduct from balance and add to locked
        sellerWallet.setBalance(sellerWallet.getBalance().subtract(amount));
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().add(amount));
        fundingWalletRepository.save(sellerWallet);

        // Create order record
        P2POrderDetail order = new P2POrderDetail();
        order.setAd(ad);
        order.setBuyerId(buyerId);
        order.setSellerId(ad.getUserId());
        order.setAsset(ad.getAsset());
        order.setFiatCurrency(ad.getFiatCurrency());
        order.setPaymentMethod(ad.getPaymentMethods().get(0));
        order.setCryptoAmount(amount);
        // Calculate fiat amount: cryptoAmount * price
        BigDecimal fiatAmount = amount.multiply(ad.getPrice());
        order.setFiatAmount(fiatAmount);
        order.setStatus(P2PTransactionStatus.AWAITING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        orderRepository.save(order);

        // Build detailed response for frontend
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("id", order.getId());
        orderData.put("buyerId", order.getBuyerId());
        orderData.put("sellerId", order.getSellerId());
        orderData.put("amount", order.getFiatAmount());
        orderData.put("cryptoAmount", order.getCryptoAmount());
        orderData.put("totalPrice", order.getFiatAmount());
        orderData.put("asset", order.getAsset());
        orderData.put("fiatCurrency", order.getFiatCurrency());
        orderData.put("paymentMethod", order.getPaymentMethod());
        orderData.put("status", order.getStatus().toString().toLowerCase());
        orderData.put("createdAt", order.getCreatedAt().toString());
        orderData.put("expiresAt", order.getExpiresAt() != null ? order.getExpiresAt().toString() : null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "code", 200,
                        "message", "Success",
                        "data", orderData));
    }

    /**
     * Get order detail - only buyer or seller can view
     */
    @Transactional
    public ResponseEntity<?> getOrderDetail(Long orderId, String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        P2POrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Validate: only buyer or seller can view
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to view this order"));
        }

        P2PAd ad = order.getAd();

        // Build payment method detail
        Map<String, Object> paymentMethodDetail = new HashMap<>();
        paymentMethodDetail.put("id", "pm_" + order.getId());
        paymentMethodDetail.put("type", order.getPaymentMethod());
        paymentMethodDetail.put("name", order.getPaymentMethod());
        // Note: You may need to add more fields to store actual bank details

        // Build order (ad) detail
        Map<String, Object> orderDetail = new HashMap<>();
        orderDetail.put("id", ad.getId());
        orderDetail.put("type", ad.getTradeType().toString().toLowerCase());
        orderDetail.put("merchantId", ad.getUserId());
        orderDetail.put("asset", ad.getAsset());
        orderDetail.put("fiatCurrency", ad.getFiatCurrency());
        orderDetail.put("price", ad.getPrice());
        orderDetail.put("minAmount", ad.getMinAmount());
        orderDetail.put("maxAmount", ad.getMaxAmount());
        orderDetail.put("availableAmount", ad.getAvailableAmount());
        orderDetail.put("terms", ad.getTermsConditions());
        orderDetail.put("paymentMethods", ad.getPaymentMethods());

        // Build main response data
        Map<String, Object> data = new HashMap<>();
        data.put("id", "trade_" + order.getId());
        data.put("orderId", "ad_" + ad.getId());
        data.put("buyerId", order.getBuyerId());
        data.put("sellerId", order.getSellerId());
        data.put("amount", order.getFiatAmount());
        data.put("cryptoAmount", order.getCryptoAmount());
        data.put("totalPrice", order.getFiatAmount());
        data.put("status", order.getStatus().toString().toLowerCase());
        data.put("createdAt", order.getCreatedAt().toString());
        data.put("expiresAt", order.getExpiresAt() != null ? order.getExpiresAt().toString() : null);
        data.put("paymentMethod", paymentMethodDetail);
        data.put("order", orderDetail);

        return ResponseEntity.ok(
                Map.of(
                        "code", 200,
                        "message", "Success",
                        "data", data));
    }

    /**
     * Seller confirms that external payment has been received.
     */
    @Transactional
    public ResponseEntity<?> confirmExternalPayment(Long orderId, String authHeader) {
        String token = authHeader.substring(7);
        String sellerId = jwtUtil.getUserIdFromToken(token);
        P2POrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getSellerId().equals(sellerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Not authorized"));
        }
        if (order.getStatus() != P2PTransactionStatus.AWAITING_PAYMENT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Order not awaiting payment"));
        }
        order.setStatus(P2PTransactionStatus.PAYMENT_SENT);
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "payment_confirmed"));
    }

    /**
     * Transfer coins after payment confirmation.
     */
    @Transactional
    public ResponseEntity<?> releaseCoins(Long orderId) {
        P2POrderDetail order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != P2PTransactionStatus.PAYMENT_SENT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Order not ready for release"));
        }
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(order.getSellerId(),
                order.getAsset());
        FundingWallet buyerWallet = fundingWalletRepository.findByUidAndCurrency(order.getBuyerId(), order.getAsset());
        if (sellerWallet == null || buyerWallet == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Wallet not found"));
        }
        if (sellerWallet.getLockedBalance().compareTo(order.getCryptoAmount()) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Seller has insufficient locked balance"));
        }
        // Release locked coins from seller and transfer to buyer
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(order.getCryptoAmount()));
        buyerWallet.setBalance(buyerWallet.getBalance().add(order.getCryptoAmount()));
        fundingWalletRepository.save(sellerWallet);
        fundingWalletRepository.save(buyerWallet);

        order.setStatus(P2PTransactionStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "coins_released", "orderId", order.getId()));
    }
}
