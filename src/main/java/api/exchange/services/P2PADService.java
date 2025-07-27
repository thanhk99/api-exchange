package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Response.UserP2PResponse;
import api.exchange.models.FundingWallet;
import api.exchange.models.P2PAd;
import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2POrderDetail.P2PTransactionStatus;
import api.exchange.models.TransactionAds;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.OrderRepository;
import api.exchange.repository.P2PAdRepository;
import api.exchange.repository.P2POrderDetailRepository;
import api.exchange.repository.TransactionsAdsRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;

@Service
public class P2PADService {

    @Autowired
    private P2PAdRepository p2pAdRepository;

    @Autowired
    private TransactionsAdsRepository transactionsAdsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private P2POrderDetailRepository orderRepository;

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    public ResponseEntity<?> createAddP2P(P2PAd p2pAd, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);
            List<P2PAd> ListP2PAd = p2pAdRepository.findByUserId(uid);
            if (ListP2PAd != null) {
                for (P2PAd p2pAdInfo : ListP2PAd) {
                    if (p2pAd.getPrice().compareTo(p2pAdInfo.getPrice()) == 0
                            && p2pAd.getPaymentMethods().equals(p2pAdInfo.getPaymentMethods())) {
                        return ResponseEntity.status(409).body(Map.of("message", "Quảng cáo này bị trùng lập"));
                    }
                }
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            LocalDateTime expiresAt = createDt.plusDays(30);
            p2pAd.setCreatedAt(createDt);
            p2pAd.setExpiresAt(expiresAt);
            p2pAd.setUserId(uid);
            p2pAdRepository.save(p2pAd);
            return ResponseEntity.status(201).body(Map.of("message", "success", "data", p2pAd));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<?> getListP2PAds() {
        List<UserP2PResponse> listP2PResponse = new ArrayList<>();
        List<P2PAd> listP2pAds = p2pAdRepository.findAll();
        for (P2PAd p2pAd : listP2pAds) {
            String name = userRepository.findById(p2pAd.getUserId()).get().getUsername();
            Long totalTransfer = transactionsAdsRepository.countCompletedTransactions((p2pAd.getUserId()));

            UserP2PResponse userP2PResponse = new UserP2PResponse(
                    p2pAd.getId(),
                    p2pAd.getUserId(),
                    name,
                    totalTransfer,
                    0,
                    0,
                    p2pAd.getPrice(),
                    p2pAd.getAvailableAmount(),
                    p2pAd.getMinAmount(),
                    p2pAd.getMaxAmount(),
                    p2pAd.getPaymentMethods(),
                    p2pAd.getAsset(),
                    p2pAd.getFiatCurrency());
            listP2PResponse.add(userP2PResponse);
        }
        return ResponseEntity.ok(Map.of("message", "success", "data", listP2PResponse));
    }

    public void lockCoinP2P(TransactionAds transactionAds) {
    try {
    FundingWallet fundingWallet =
    fundingWalletRepository.findByUid(transactionAds.getFromUser());
    fundingWallet.setLockedBalance(transactionAds.getCoinAmount());
    } catch (Exception e) {
    System.out.println(e);
    }
    }

    /**
     * Bước 1 & 2: Người mua tạo lệnh P2P & Hệ thống tạm giữ coin (escrow).
     * 
     * @param buyerId      ID của người mua (lấy từ token).
     * @param adId         ID của quảng cáo muốn mua.
     * @param cryptoAmount Số lượng coin muốn mua.
     * @return Giao dịch vừa được tạo.
     */

    @Transactional
    public ResponseEntity<?> createTransaction(String buyerId, Long adId, BigDecimal cryptoAmount) {
        // Lấy thông tin quảng cáo
        P2PAd ad = p2pAdRepository.findById(adId)
                .orElseThrow(() -> new IllegalStateException("Quảng cáo không tồn tại."));

        String sellerId = ad.getUserId();

        // Kiểm tra xem người mua có tự mua của chính mình không
        if (buyerId.equals(sellerId)) {
            throw new IllegalStateException("Bạn không thể tự tạo giao dịch với quảng cáo của chính mình.");
        }

        // Kiểm tra số lượng mua có hợp lệ không
        if (cryptoAmount.compareTo(ad.getMinAmount()) < 0 || cryptoAmount.compareTo(ad.getAvailableAmount()) > 0) {
            throw new IllegalStateException("Số lượng mua không hợp lệ.");
        }

        // Lấy ví funding của người bán để khóa coin
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(sellerId, ad.getFiatCurrency());
        if (sellerWallet == null) {
            throw new IllegalStateException("Ví của người bán không tìm thấy.");
        }

        // Kiểm tra số dư khả dụng của người bán
        if (sellerWallet.getBalance().compareTo(cryptoAmount) < 0) {
            throw new IllegalStateException("Người bán không đủ số dư khả dụng.");
        }

        // **Bước 2: OKX tạm giữ coin**
        // Trừ tiền khỏi số dư chính và cộng vào số dư bị khóa
        sellerWallet.setBalance(sellerWallet.getBalance().subtract(cryptoAmount));
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().add(cryptoAmount));
        fundingWalletRepository.save(sellerWallet);

        // Cập nhật lại số lượng còn lại của quảng cáo
        ad.setAvailableAmount(ad.getAvailableAmount().subtract(cryptoAmount));
        p2pAdRepository.save(ad);

        // **Bước 1: Người mua tạo lệnh P2P**
        P2POrderDetail transaction = new P2POrderDetail();
        transaction.setAd(ad);
        transaction.setBuyerId(buyerId);
        transaction.setSellerId(sellerId);
        transaction.setAsset(ad.getAsset());
        transaction.setFiatCurrency(ad.getFiatCurrency());
        transaction.setCryptoAmount(cryptoAmount);
        transaction.setFiatAmount(cryptoAmount.multiply(ad.getPrice())); // Tính toán số tiền fiat
        transaction.setPaymentMethod(ad.getPaymentMethods());
        transaction.setStatus(P2POrderDetail.P2PTransactionStatus.AWAITING_PAYMENT); // Chuyển trạng thái chờ thanh toán, dùng enum
        transaction.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        transaction.setExpiresAt(transaction.getCreatedAt().plusMinutes(15)); // Hẹn giờ hủy sau 15 phút

        orderRepository.save(transaction);
        return ResponseEntity.status(201).body(Map.of("message", "success", "data", transaction));
    }

    /**
     * Bước 3: Người mua xác nhận đã chuyển tiền.
     * @param transactionId ID của giao dịch.
     * @param buyerId ID của người mua để xác thực.
     * @return Giao dịch đã được cập nhật.
     */
    @Transactional
    public ResponseEntity<?> confirmPayment(Long transactionId, String buyerId) {
        P2POrderDetail transaction = orderRepository.findByIdAndBuyerId(transactionId, buyerId);
        if (transaction == null) {
            throw new IllegalStateException("Giao dịch không tồn tại hoặc bạn không phải người mua.");
        }

        if (transaction.getStatus() != P2POrderDetail.P2PTransactionStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Giao dịch không ở trạng thái chờ thanh toán.");
        }

        transaction.setStatus(P2POrderDetail.P2PTransactionStatus.PAYMENT_SENT); // Cập nhật trạng thái "Đã chuyển tiền"
        orderRepository.save(transaction);
        return ResponseEntity.ok(Map.of("message", "success", "data", transaction));
    }

    /**
     * Bước 4: Người bán xác nhận nhận tiền và giải phóng coin.
     * Đây là trường hợp "Tiền đã nhận đúng?" -> "Có".
     * @param transactionId ID của giao dịch.
     * @param sellerId ID của người bán để xác thực.
     * @return Giao dịch hoàn tất.
     */
    @Transactional
    public ResponseEntity<?> releaseCoins(Long transactionId, String sellerId) {
        P2POrderDetail transaction = orderRepository.findByIdAndSellerId(transactionId, sellerId);
        if (transaction == null) {
            throw new IllegalStateException("Giao dịch không tồn tại hoặc bạn không phải người bán.");
        }

        if (transaction.getStatus() != P2POrderDetail.P2PTransactionStatus.PAYMENT_SENT) {
            throw new IllegalStateException("Giao dịch chưa được người mua xác nhận thanh toán.");
        }

        // Lấy ví của người bán và người mua
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(sellerId, transaction.getAsset());
        FundingWallet buyerWallet = fundingWalletRepository.findByUidAndCurrency(transaction.getBuyerId(), transaction.getAsset());
        if (buyerWallet == null) {
            buyerWallet = new FundingWallet();
            buyerWallet.setUid(transaction.getBuyerId());
            buyerWallet.setCurrency(transaction.getAsset());
            buyerWallet.setBalance(BigDecimal.ZERO);
            buyerWallet.setLockedBalance(BigDecimal.ZERO);
        }
        
        // **OKX giải phóng coin cho người mua**
        // Trừ số dư bị khóa của người bán
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(transaction.getCryptoAmount()));
        // Cộng vào số dư chính của người mua
        buyerWallet.setBalance(buyerWallet.getBalance().add(transaction.getCryptoAmount()));

        fundingWalletRepository.save(sellerWallet);
        fundingWalletRepository.save(buyerWallet);

        // **Giao dịch hoàn tất**
        transaction.setStatus(P2POrderDetail.P2PTransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        return ResponseEntity.ok(orderRepository.save(transaction));
    }

    /**
     * Bước 5: Mở tranh chấp (Khiếu nại).
     * Đây là trường hợp "Tiền đã nhận đúng?" -> "Không".
     * @param transactionId ID của giao dịch.
     * @param userId ID của người mở tranh chấp (có thể là người mua hoặc người bán).
     * @return Giao dịch đã được cập nhật trạng thái tranh chấp.
     */
    @Transactional
    public ResponseEntity<?> openDispute(Long transactionId, String userId) {
        P2POrderDetail transaction = orderRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Giao dịch không tồn tại."));
        
        // Kiểm tra người dùng có phải là một phần của giao dịch không
        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền truy cập vào giao dịch này.");
        }

        transaction.setStatus(P2POrderDetail.P2PTransactionStatus.DISPUTE_OPENED);
        // Có thể thêm logic gửi thông báo cho admin tại đây.
        return ResponseEntity.ok(orderRepository.save(transaction));
    }


}
