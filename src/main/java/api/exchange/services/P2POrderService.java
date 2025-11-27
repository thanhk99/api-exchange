package api.exchange.services;

import api.exchange.models.P2PAd;
import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2POrderDetail.P2PTransactionStatus;
import api.exchange.models.FundingWallet;
import api.exchange.repository.P2PAdRepository;
import api.exchange.repository.P2POrderDetailRepository;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.PaymentMethodRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        @Autowired
        private PaymentMethodRepository paymentMethodRepository;

        /**
         * Buyer creates an order on an existing ad.
         */
        @Transactional
        public ResponseEntity<?> createOrder(Long adId, BigDecimal amount, String authHeader) {
                String token = authHeader.substring(7);
                String takerId = jwtUtil.getUserIdFromToken(token);
                P2PAd ad = p2pAdRepository.findById(adId)
                                .orElseThrow(() -> new IllegalArgumentException("Ad not found"));
                if (!ad.getIsActive()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", "Ad is not active"));
                }

                String buyerId;
                String sellerId;

                // Determine Buyer and Seller based on Ad Type
                if (ad.getTradeType() == P2PAd.TradeType.SELL) {
                        // Ad is SELL: Advertiser is Seller, Taker is Buyer
                        sellerId = ad.getUserId();
                        buyerId = takerId;
                } else {
                        // Ad is BUY: Advertiser is Buyer, Taker is Seller
                        buyerId = ad.getUserId();
                        sellerId = takerId;
                }

                // Requirement: Seller must have at least one payment method to receive money
                // If Ad is BUY -> Taker is Seller -> Check Taker's payment methods
                if (ad.getTradeType() == P2PAd.TradeType.BUY) {
                        long paymentMethodCount = paymentMethodRepository.countByUserIdAndIsActiveTrue(sellerId);
                        if (paymentMethodCount == 0) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of("message",
                                                                "Bạn cần thêm tài khoản nhận tiền trước khi bán coin"));
                        }
                }

                if (ad.getAvailableAmount().compareTo(amount) < 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", "Insufficient amount in ad"));
                }
                // Lock coins in Seller's Funding wallet
                // If SELL Ad: Funds already locked when Ad was created.
                // If BUY Ad: Taker is Seller, need to lock funds now.
                if (ad.getTradeType() == P2PAd.TradeType.BUY) {
                        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(sellerId,
                                        ad.getAsset());
                        if (sellerWallet == null) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of("message", "Seller wallet not found"));
                        }
                        if (sellerWallet.getBalance().compareTo(amount) < 0) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of("message", "Seller has insufficient balance"));
                        }
                        // Deduct from balance and add to locked
                        sellerWallet.setBalance(sellerWallet.getBalance().subtract(amount));
                        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().add(amount));
                        fundingWalletRepository.save(sellerWallet);
                }

                // Reduce available amount on the ad
                ad.setAvailableAmount(ad.getAvailableAmount().subtract(amount));
                p2pAdRepository.save(ad);

                // Create order record
                P2POrderDetail order = new P2POrderDetail();
                order.setAd(ad);
                order.setBuyerId(buyerId);
                order.setSellerId(sellerId);
                order.setAsset(ad.getAsset());
                order.setFiatCurrency(ad.getFiatCurrency());
                if (ad.getPaymentMethods() == null || ad.getPaymentMethods().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", "Ad has no payment methods"));
                }
                // Note: For BUY ads, this picks the first accepted type.
                // Ideally Taker should select/provide their payment method details.
                order.setPaymentMethod(ad.getPaymentMethods().get(0));
                order.setCryptoAmount(amount);
                // Calculate fiat amount: cryptoAmount * price
                BigDecimal fiatAmount = amount.multiply(ad.getPrice());
                order.setFiatAmount(fiatAmount);
                order.setStatus(P2PTransactionStatus.AWAITING_PAYMENT);
                LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                order.setCreatedAt(createdAt);
                order.setExpiresAt(createdAt.plusMinutes(15));
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

                if (ad.getPaymentMethodId() != null) {
                        paymentMethodRepository.findById(ad.getPaymentMethodId()).ifPresent(pm -> {
                                paymentMethodDetail.put("accountName", pm.getAccountName());
                                paymentMethodDetail.put("accountNumber", pm.getAccountNumber());
                                paymentMethodDetail.put("bankName", pm.getBankName());
                                paymentMethodDetail.put("branch", pm.getBranchName());
                                paymentMethodDetail.put("qrCode", pm.getQrCode());
                        });
                } else {
                        // For BUY ads, Taker is Seller. Find their payment method matching the order
                        // type.
                        List<api.exchange.models.PaymentMethod> sellerMethods = paymentMethodRepository
                                        .findByUserIdAndIsActiveTrue(order.getSellerId());

                        // Find first method matching the type string in order.getPaymentMethod()
                        sellerMethods.stream()
                                        .filter(pm -> pm.getType().name().equals(order.getPaymentMethod()))
                                        .findFirst()
                                        .ifPresentOrElse(pm -> {
                                                paymentMethodDetail.put("accountName", pm.getAccountName());
                                                paymentMethodDetail.put("accountNumber", pm.getAccountNumber());
                                                paymentMethodDetail.put("bankName", pm.getBankName());
                                                paymentMethodDetail.put("branch", pm.getBranchName());
                                                paymentMethodDetail.put("qrCode", pm.getQrCode());
                                        }, () -> {
                                                // Fallback if no specific method found (shouldn't happen given
                                                // createOrder checks)
                                                paymentMethodDetail.put("name", order.getPaymentMethod());
                                        });
                }

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
         * Buyer confirms payment has been sent (within 15 minutes)
         */
        @Transactional
        public ResponseEntity<?> buyerConfirmPayment(Long orderId, String authHeader) {
                String token = authHeader.substring(7);
                String buyerId = jwtUtil.getUserIdFromToken(token);
                P2POrderDetail order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                System.out.println("DEBUG: buyerConfirmPayment - OrderID: " + orderId);
                System.out.println("DEBUG: buyerConfirmPayment - Token BuyerID: " + buyerId);
                System.out.println("DEBUG: buyerConfirmPayment - Order BuyerID: " + order.getBuyerId());

                if (!order.getBuyerId().equals(buyerId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("message", "Not authorized"));
                }

                if (order.getStatus() != P2PTransactionStatus.AWAITING_PAYMENT) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", "Order not awaiting payment"));
                }

                // Check if order has expired (15 minutes)
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                if (order.getExpiresAt() != null && now.isAfter(order.getExpiresAt())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message",
                                                        "Order has expired. Payment confirmation not accepted."));
                }

                order.setStatus(P2PTransactionStatus.PAYMENT_SENT);
                orderRepository.save(order);
                return ResponseEntity.ok(Map.of("message", "Payment confirmation received"));
        }

        /**
         * Seller confirms payment received and releases coins to buyer
         */
        @Transactional
        public ResponseEntity<?> sellerConfirmPaymentReceived(Long orderId, String authHeader) {
                String token = authHeader.substring(7);
                String sellerId = jwtUtil.getUserIdFromToken(token);

                P2POrderDetail order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                if (!order.getSellerId().equals(sellerId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("message", "Not authorized"));
                }

                if (order.getStatus() != P2PTransactionStatus.PAYMENT_SENT) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", "Buyer has not confirmed payment yet"));
                }

                FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(order.getSellerId(),
                                order.getAsset());
                FundingWallet buyerWallet = fundingWalletRepository.findByUidAndCurrency(order.getBuyerId(),
                                order.getAsset());

                // Ensure both seller and buyer wallets exist; create if missing
                if (sellerWallet == null) {
                        sellerWallet = new FundingWallet();
                        sellerWallet.setUid(order.getSellerId());
                        sellerWallet.setCurrency(order.getAsset());
                        sellerWallet.setBalance(BigDecimal.ZERO);
                        sellerWallet.setLockedBalance(BigDecimal.ZERO);
                        // isActive defaults to true
                        fundingWalletRepository.save(sellerWallet);
                }
                if (buyerWallet == null) {
                        buyerWallet = new FundingWallet();
                        buyerWallet.setUid(order.getBuyerId());
                        buyerWallet.setCurrency(order.getAsset());
                        buyerWallet.setBalance(BigDecimal.ZERO);
                        buyerWallet.setLockedBalance(BigDecimal.ZERO);
                        fundingWalletRepository.save(buyerWallet);
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

                return ResponseEntity.ok(Map.of(
                                "message", "Payment confirmed and coins released",
                                "orderId", order.getId()));
        }

        /**
         * Get user P2P profile statistics
         */
        public ResponseEntity<?> getUserProfile(String authHeader) {
                String token = authHeader.substring(7);
                String userId = jwtUtil.getUserIdFromToken(token);

                Long totalTransactions = orderRepository.countTotalTransactions(userId);
                Long completedTransactions = orderRepository.countTransactionsByStatus(userId,
                                P2PTransactionStatus.COMPLETED);

                double completionRate = totalTransactions > 0 ? (completedTransactions * 100.0 / totalTransactions)
                                : 0.0;

                Map<String, Object> profileData = new HashMap<>();
                profileData.put("userId", userId);
                profileData.put("totalTransactions", totalTransactions);
                profileData.put("completedTransactions", completedTransactions);
                profileData.put("completionRate", String.format("%.2f", completionRate) + "%");
                profileData.put("rating", 0); // TODO: Implement rating system

                return ResponseEntity.ok(Map.of(
                                "code", 200,
                                "message", "Success",
                                "data", profileData));
        }

        /**
         * Get user transaction history
         */
        public ResponseEntity<?> getUserTransactionHistory(String authHeader) {
                String token = authHeader.substring(7);
                String userId = jwtUtil.getUserIdFromToken(token);

                List<P2POrderDetail> transactions = orderRepository.findUserTransactionHistory(userId);

                List<Map<String, Object>> historyData = new ArrayList<>();
                for (P2POrderDetail transaction : transactions) {
                        Map<String, Object> txData = new HashMap<>();
                        txData.put("id", transaction.getId());
                        txData.put("adId", transaction.getAd().getId());
                        txData.put("type", transaction.getBuyerId().equals(userId) ? "BUY" : "SELL");
                        txData.put("asset", transaction.getAsset());
                        txData.put("fiatCurrency", transaction.getFiatCurrency());
                        txData.put("cryptoAmount", transaction.getCryptoAmount());
                        txData.put("fiatAmount", transaction.getFiatAmount());
                        txData.put("status", transaction.getStatus().toString().toLowerCase());
                        txData.put("paymentMethod", transaction.getPaymentMethod());
                        txData.put("createdAt", transaction.getCreatedAt().toString());
                        txData.put("completedAt",
                                        transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString()
                                                        : null);

                        // Add counterparty info
                        String counterparty = transaction.getBuyerId().equals(userId) ? transaction.getSellerId()
                                        : transaction.getBuyerId();
                        txData.put("counterparty", counterparty);

                        historyData.add(txData);
                }

                return ResponseEntity.ok(Map.of(
                                "code", 200,
                                "message", "Success",
                                "data", historyData));
        }

        /**
         * Cancel a P2P order
         * Security: Only buyer or seller can cancel their own order
         * Business: Only orders in AWAITING_PAYMENT status can be cancelled
         * Rollback: Unlock seller's coins and restore ad's available amount
         */
        @Transactional
        public ResponseEntity<?> cancelOrder(Long orderId, String authHeader) {
                // Authentication: Extract user ID from JWT token
                String token = authHeader.substring(7);
                String userId = jwtUtil.getUserIdFromToken(token);

                // Find order or throw exception
                P2POrderDetail order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                // Authorization: Only buyer or seller can cancel the order
                if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("message", "You are not authorized to cancel this order"));
                }

                // State Validation: Only allow cancellation if order is in AWAITING_PAYMENT
                // status
                if (order.getStatus() != P2PTransactionStatus.AWAITING_PAYMENT) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message",
                                                        "Cannot cancel order in current status: " + order.getStatus()));
                }

                // Get the ad and seller wallet for rollback
                P2PAd ad = order.getAd();
                FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(
                                order.getSellerId(), order.getAsset());

                if (sellerWallet == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("message", "Seller wallet not found"));
                }

                // Rollback 1: Unlock seller's coins
                BigDecimal lockedAmount = order.getCryptoAmount();

                // If SELL Ad: Funds belong to Ad, so keep them locked (do nothing here).
                // If BUY Ad: Taker gets their funds back.
                if (ad.getTradeType() == P2PAd.TradeType.BUY) {
                        if (sellerWallet.getLockedBalance().compareTo(lockedAmount) < 0) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("message", "Insufficient locked balance for rollback"));
                        }

                        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(lockedAmount));
                        sellerWallet.setBalance(sellerWallet.getBalance().add(lockedAmount));
                        fundingWalletRepository.save(sellerWallet);
                }

                // Rollback 2: Restore ad's available amount
                ad.setAvailableAmount(ad.getAvailableAmount().add(lockedAmount));
                p2pAdRepository.save(ad);

                order.setStatus(P2PTransactionStatus.CANCELLED);
                order.setCompletedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                orderRepository.save(order);

                return ResponseEntity.ok(Map.of(
                                "code", 200,
                                "message", "Order cancelled successfully",
                                "data", Map.of(
                                                "orderId", order.getId(),
                                                "status", order.getStatus().toString().toLowerCase(),
                                                "cancelledAt", order.getCompletedAt().toString())));
        }
}
