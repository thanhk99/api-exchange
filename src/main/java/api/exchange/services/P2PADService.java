package api.exchange.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Response.ProfileP2PResponse;
import api.exchange.dtos.Response.UserP2PResponse;
import api.exchange.models.FundingWallet;
import api.exchange.models.P2PAd;
import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2PAd.TradeType;
import api.exchange.repository.FundingWalletRepository;
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
                            && p2pAd.getPaymentMethods().equals(p2pAdInfo.getPaymentMethods()) && p2pAd.getTradeType().equals(p2pAdInfo.getTradeType()) && p2pAd.getAsset().equals(p2pAdInfo.getAsset())) {
                        return ResponseEntity.status(409).body(Map.of("message", "Quảng cáo này bị trùng lập"));
                    }
                }
            }
            if (p2pAd.getTradeType().equals(TradeType.SELL)) {
                FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(uid, p2pAd.getAsset());
                if (sellerWallet == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Ví Funding bạn không có loại tiền này "));
                }
                if(sellerWallet.getBalance().compareTo(p2pAd.getAvailableAmount()) < 0){
                    return ResponseEntity.badRequest().body(Map.of("message" , "Số dư trong ví Funding của bạn không đủ "));
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

    public ResponseEntity<?> getListP2PAds(TradeType type) {
        List<UserP2PResponse> listP2PResponse = new ArrayList<>();
        List<P2PAd> listP2pAds = p2pAdRepository.findByTradeType(type);
        for (P2PAd p2pAd : listP2pAds) {
            String name = userRepository.findById(p2pAd.getUserId()).get().getUsername();
            BigDecimal totalTransfer = transactionsAdsRepository.countTotalTransactions((p2pAd.getUserId()));
            BigDecimal doneTransfer = transactionsAdsRepository.countCompletedTransactions(p2pAd.getUserId());
            BigDecimal percentComplete = doneTransfer.divide(totalTransfer, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
            UserP2PResponse userP2PResponse = new UserP2PResponse(
                    p2pAd.getId(),
                    p2pAd.getUserId(),
                    name,
                    totalTransfer,
                    percentComplete,
                    0,
                    p2pAd.getPrice(),
                    p2pAd.getAvailableAmount(),
                    p2pAd.getMinAmount(),
                    p2pAd.getMaxAmount(),
                    p2pAd.getPaymentMethods(),
                    p2pAd.getAsset(),
                    p2pAd.getFiatCurrency(),
                    p2pAd.getTradeType());
            listP2PResponse.add(userP2PResponse);
        }
        return ResponseEntity.ok(Map.of("message", "success", "data", listP2PResponse));
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

    public ResponseEntity<?> profileUserP2P(String header){
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        //30 days
        LocalDateTime endDate= LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime startDate= endDate.minusDays(30);
        BigDecimal countOrderDone30Days = transactionsAdsRepository.countCompletedTransactions30Days(uid, startDate, endDate);
        BigDecimal buyOrderDone30Days = transactionsAdsRepository.countCompletedBuyTransactions30Days(uid, startDate, endDate);
        BigDecimal sellOrderDone30Days = countOrderDone30Days.subtract(buyOrderDone30Days);

        BigDecimal totalTrans30Days = transactionsAdsRepository.countTotalTransactions30Days(uid,startDate,endDate);
        BigDecimal totalTrans = transactionsAdsRepository.countTotalTransactions(uid);

        BigDecimal percnetTotalDone30Days = countOrderDone30Days.divide(totalTrans30Days, 2, RoundingMode.HALF_UP);
        BigDecimal percnetBuyDone30Days = buyOrderDone30Days.divide(countOrderDone30Days , 2 ,  RoundingMode.HALF_UP);
        BigDecimal percnetSellDone30Days = sellOrderDone30Days.divide(countOrderDone30Days,2 , RoundingMode.HALF_UP);
        
        BigDecimal totalOrderDone = transactionsAdsRepository.countCompletedTransactions(uid);
        BigDecimal percnetTotalDone = totalOrderDone.divide(totalTrans , 2 ,RoundingMode.HALF_UP);

        FundingWallet fundingWallet = fundingWalletRepository.findByUid(uid);
        BigDecimal coinAvaiable = fundingWallet.getBalance();
        BigDecimal coinLocked = fundingWallet.getLockedBalance();

        return ResponseEntity.ok(Map.of("message","success", "data" , new ProfileP2PResponse(
            coinAvaiable,
            coinLocked,
            countOrderDone30Days,
            buyOrderDone30Days,
            sellOrderDone30Days,
            percnetTotalDone30Days,
            percnetBuyDone30Days,
            percnetSellDone30Days,
            totalOrderDone, 
            new BigDecimal(0),
            percnetTotalDone,
            new BigDecimal(0),
            new BigDecimal(0),
            new BigDecimal(0),
            new BigDecimal(0),
            new BigDecimal(0),
            new BigDecimal(0)
        )));


   }

}
