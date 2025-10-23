package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.SpotWallet;
import api.exchange.models.SpotHistory.TradeType;
import api.exchange.models.SpotWalletHistory;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.SpotWalletRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SpotWalletService {

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Transactional
    public ResponseEntity<?> addBalanceCoin(SpotWallet entity) {
        try {
            SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
            SpotWallet existingWallet = spotWalletRepository.findByUidAndCurrency(
                    entity.getUid(),
                    entity.getCurrency());
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(entity.getBalance()));
                spotWalletHistory.setBalance(existingWallet.getBalance());
                spotWalletRepository.save(existingWallet);
            } else {
                entity.setUid(entity.getUid());
                spotWalletHistory.setBalance(entity.getBalance());
                spotWalletRepository.save(entity);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            spotWalletHistory.setUserId(entity.getUid());
            spotWalletHistory.setAsset(entity.getCurrency());
            spotWalletHistory.setType("Nạp tiền");
            spotWalletHistory.setBalance(entity.getBalance());
            spotWalletHistory.setCreateDt(createDt);
            spotWalletHistoryRepository.save(spotWalletHistory);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    @Transactional
    public void executeTradeSpot(String sellerUid, String buyerUid, BigDecimal price, BigDecimal quantity,
            TradeType tradeType, String symbol, BigDecimal lockedPrice) {

        String[] parts = symbol.split("/");
        String coin = parts[0];
        String stableCoin = parts[1];

        BigDecimal totalCost = price.multiply(quantity);
        BigDecimal lockedCost = lockedPrice.multiply(quantity);
        BigDecimal excessAmount = lockedCost.subtract(totalCost);

        // Lấy ví với pessimistic lock để tránh race condition
        SpotWallet spotBuyerReceive = spotWalletRepository.findByUidAndCurrency(buyerUid, coin);
        SpotWallet spotSellerSend = spotWalletRepository.findByUidAndCurrency(sellerUid, coin);
        SpotWallet spotSellerReceive = spotWalletRepository.findByUidAndCurrency(sellerUid, stableCoin);
        SpotWallet spotBuyerSend = spotWalletRepository.findByUidAndCurrency(buyerUid, stableCoin);

        // Xử lý theo loại trade
        switch (tradeType) {
            case LIMIT_LIMIT:
                // Trừ từ locked balance
                spotBuyerSend.setLockedBalance(spotBuyerSend.getLockedBalance().subtract(totalCost));
                spotSellerSend.setLockedBalance(spotSellerSend.getLockedBalance().subtract(quantity));
                if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
                    spotBuyerSend.setBalance(spotBuyerSend.getBalance().add(excessAmount));
                }
                break;

            case MARKET_LIMIT_BUY:
                // Buyer dùng market (balance), seller dùng limit (locked)
                spotBuyerSend.setBalance(spotBuyerSend.getBalance().subtract(totalCost));
                spotSellerSend.setLockedBalance(spotSellerSend.getLockedBalance().subtract(quantity));
                break;

            case MARKET_LIMIT_SELL:
                // Buyer dùng limit (locked), seller dùng market (balance)
                spotBuyerSend.setLockedBalance(spotBuyerSend.getLockedBalance().subtract(totalCost));
                spotSellerSend.setBalance(spotSellerSend.getBalance().subtract(quantity));

                break;

            case MARKET_MARKET:
                // Cả hai đều dùng balance
                spotBuyerSend.setBalance(spotBuyerSend.getBalance().subtract(totalCost));
                spotSellerSend.setBalance(spotSellerSend.getBalance().subtract(quantity));
                break;
        }

        // Cộng tiền cho người nhận (LUÔN cộng vào balance)
        spotBuyerReceive.setBalance(spotBuyerReceive.getBalance().add(quantity));
        spotSellerReceive.setBalance(spotSellerReceive.getBalance().add(totalCost));
        // Ghi nhận biến động số dư
        balanceFluctuation(buyerUid, coin, quantity, "Nhận coin từ giao dịch");
        balanceFluctuation(sellerUid, stableCoin, totalCost, "Nhận tiền từ giao dịch");
        balanceFluctuation(buyerUid, stableCoin, totalCost.negate(), "Trừ tiền mua coin");
        balanceFluctuation(sellerUid, coin, quantity.negate(), "Trừ coin bán");
        
        // Lưu tất cả ví
        spotWalletRepository.saveAll(Arrays.asList(
                spotBuyerReceive, spotSellerSend, spotSellerReceive, spotBuyerSend));

        log.info("✅ Trade executed: {} {} @ {} - Buyer: {}, Seller: {}",
                quantity, coin, price, buyerUid, sellerUid);
    }

    @Transactional
    public void balanceFluctuation(String uid, String currency, BigDecimal amount, String type) {
        SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
        spotWalletHistory.setUserId(uid);
        spotWalletHistory.setAsset(currency);
        spotWalletHistory.setType(type);
        spotWalletHistory.setBalance(amount);
        LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        spotWalletHistory.setCreateDt(createDt);
        spotWalletHistoryRepository.save(spotWalletHistory);
    }
}
