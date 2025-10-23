package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import api.exchange.models.FundingWallet;
import api.exchange.models.P2PAd;
import api.exchange.models.TransactionAds;
import api.exchange.models.TransactionAds.status;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.P2PAdRepository;
import api.exchange.repository.TransactionsAdsRepository;

@Service
public class TransactionsAdsService {
    @Autowired
    private TransactionsAdsRepository transactionsAdsRepository;

    @Autowired 
    private P2PAdRepository p2pAdRepository ;

    @Autowired 
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private FundingWalletService fundingWalletService; 

    /**
     * Bước 1 & 2: Người mua tạo lệnh P2P & Hệ thống tạm giữ coin (escrow).
     * 
     * @param buyerId      ID của người mua (lấy từ token).
     * @param adId         ID của quảng cáo muốn mua.
     * @param cryptoAmount Số lượng coin muốn mua.
     * @return Giao dịch vừa được tạo.
     */

    @Transactional
    public ResponseEntity<?> createTransaction(TransactionAds transactionAds) {
        // Lấy thông tin quảng cáo
        P2PAd ad = p2pAdRepository.findByIdAndIsActive(transactionAds.getAdsId(),true);
        if(ad == null){
            return ResponseEntity.badRequest().body(Map.of("message","Quảng cáo không tồn tại."));
        }

        String sellerId = ad.getUserId();

        // Kiểm tra xem người mua có tự mua của chính mình không
        if (transactionAds.getBuyerId().equals(sellerId)) {
            return ResponseEntity.badRequest().body(Map.of("message","Bạn không thể tự tạo giao dịch với quảng cáo của chính mình."));
        }

        // Kiểm tra số lượng mua có hợp lệ không
        if (transactionAds.getCoinAmount().compareTo(ad.getAvailableAmount()) > 0 ) {
           return ResponseEntity.badRequest().body(Map.of("message","Số lượng mua không hợp lệ."));
        }

        // Lấy ví funding của người bán để khóa coin
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(sellerId, ad.getAsset());
        if (sellerWallet == null) {
           return ResponseEntity.badRequest().body(Map.of("message","Ví của người bán không tìm thấy."));
        }

        // Kiểm tra số dư khả dụng của người bán
        if (sellerWallet.getBalance().compareTo(transactionAds.getCoinAmount()) < 0) {
           return ResponseEntity.badRequest().body(Map.of("message","Người bán không đủ số dư khả dụng."));
        }

        // **Bước 2: OKX tạm giữ coin**
        // Trừ tiền khỏi số dư chính và cộng vào số dư bị khóa
        sellerWallet.setBalance(sellerWallet.getBalance().subtract(transactionAds.getCoinAmount()));
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().add(transactionAds.getCoinAmount()));
        fundingWalletRepository.save(sellerWallet);

        // Cập nhật lại số lượng còn lại của quảng cáo
        ad.setAvailableAmount(ad.getAvailableAmount().subtract(transactionAds.getCoinAmount()));
        p2pAdRepository.save(ad);

        // **Bước 1: Người mua tạo lệnh P2P**
        transactionAds.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        transactionsAdsRepository.save(transactionAds);
        return ResponseEntity.status(201).body(Map.of("message", "success", "data", transactionAds));
    }

    @Transactional
    public ResponseEntity<?> cancleTransP2PBy(Long idTransaction , String cancleBy) {
        try {
            TransactionAds transactionAds = transactionsAdsRepository.findById(idTransaction).get();
            if(transactionAds == null){
                return ResponseEntity.badRequest().body(Map.of("message","Giao dịch không tồn tại ."));
            }
            if (transactionAds.getStatus() != TransactionAds.status.PENDING) {
                return ResponseEntity.badRequest().body(Map.of("message","Giao dịch đã hoàn tất ."));
            }
            FundingWallet sellerWallet= fundingWalletRepository.findByUidAndCurrency(transactionAds.getSellerId(), transactionAds.getAsset());

            // Hoàn trả lại coin cho người bán 
            sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(transactionAds.getCoinAmount()));
            sellerWallet.setBalance(sellerWallet.getBalance().add(transactionAds.getCoinAmount()));

            // Chuyển trạng thái huỷ giao dịch 
            transactionAds.setStatus(status.CANCLELED);
            transactionAds.setCompleteAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            transactionAds.setCancleBy(cancleBy);

            // save
            transactionsAdsRepository.save(transactionAds);
            return ResponseEntity.ok(Map.of("message", "Cancle Success"));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "SERVER_ERROR"));
        }
    }

    @Transactional
    public ResponseEntity<?> releaseCoins(Long transactionId) {
        TransactionAds transaction = transactionsAdsRepository.findById(transactionId).get();
        if (transaction == null) {
           return ResponseEntity.badRequest().body(Map.of("message","Giao dịch không tồn tại ."));
        }

        if (transaction.getStatus() != TransactionAds.status.PENDING) {
           return ResponseEntity.badRequest().body(Map.of("message","Giao dịch chưa được người mua xác nhận thanh toán."));
        }

        // Lấy ví của người bán và người mua
        FundingWallet sellerWallet = fundingWalletRepository.findByUidAndCurrency(transaction.getSellerId(), transaction.getAsset());
        FundingWallet buyerWallet = fundingWalletRepository.findByUidAndCurrency(transaction.getBuyerId(), transaction.getAsset());
        if (buyerWallet == null) {
            buyerWallet = new FundingWallet();
            buyerWallet.setUid(transaction.getBuyerId());
            buyerWallet.setCurrency(transaction.getAsset());
            buyerWallet.setBalance(BigDecimal.ZERO);
            fundingWalletService.addBalanceCoin(buyerWallet);
        }
        
        // **OKX giải phóng coin cho người mua**
        // Trừ số dư bị khóa của người bán
        sellerWallet.setLockedBalance(sellerWallet.getLockedBalance().subtract(transaction.getCoinAmount()));
        // Cộng vào số dư chính của người mua
        buyerWallet.setBalance(buyerWallet.getBalance().add(transaction.getCoinAmount()));

        fundingWalletRepository.save(sellerWallet);
        fundingWalletRepository.save(buyerWallet);

        // **Giao dịch hoàn tất**
        transaction.setStatus(TransactionAds.status.DONE);
        transaction.setCompleteAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        transactionsAdsRepository.save(transaction);
        return ResponseEntity.ok(Map.of("message" , "transaction complete"));
    }

}
