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
import api.exchange.models.OrderBooks;
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
            if (entity.getBalance() == null || entity.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Balance must be greater than 0"));
            }

            SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
            SpotWallet existingWallet = spotWalletRepository.findByUidAndCurrency(
                    entity.getUid(),
                    entity.getCurrency());

            BigDecimal postBalance;
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(entity.getBalance()));
                postBalance = existingWallet.getBalance();
                spotWalletRepository.save(existingWallet);
            } else {
                entity.setUid(entity.getUid());
                postBalance = entity.getBalance();
                spotWalletRepository.save(entity);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            spotWalletHistory.setUserId(entity.getUid());
            spotWalletHistory.setAsset(entity.getCurrency());
            spotWalletHistory.setType("Nạp tiền");
            spotWalletHistory.setAmount(entity.getBalance());
            spotWalletHistory.setBalance(postBalance);
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

        // Use pessimistic write lock to prevent race conditions
        SpotWallet spotBuyerCoin = spotWalletRepository.findByUidAndCurrencyWithLock(buyerUid, coin);
        SpotWallet spotSellerCoin = spotWalletRepository.findByUidAndCurrencyWithLock(sellerUid, coin);
        SpotWallet spotBuyerStable = spotWalletRepository.findByUidAndCurrencyWithLock(buyerUid, stableCoin);
        SpotWallet spotSellerStable = spotWalletRepository.findByUidAndCurrencyWithLock(sellerUid, stableCoin);

        // Ensure wallets exist (should be guaranteed by checkWalletRecive but good to
        // be safe)
        if (spotBuyerCoin == null || spotSellerCoin == null || spotBuyerStable == null || spotSellerStable == null) {
            log.error("❌ Wallet not found for trade execution. Buyer: {}, Seller: {}, Symbol: {}", buyerUid, sellerUid,
                    symbol);
            throw new RuntimeException("Wallet not found for trade execution");
        }

        switch (tradeType) {
            case LIMIT_LIMIT:
                spotBuyerStable.setLockedBalance(spotBuyerStable.getLockedBalance().subtract(totalCost));

                BigDecimal buyerLockedAmount = lockedPrice.multiply(quantity);
                if (buyerLockedAmount.compareTo(totalCost) > 0) {
                    spotBuyerStable.setLockedBalance(
                            spotBuyerStable.getLockedBalance().subtract(buyerLockedAmount.subtract(totalCost)));
                    spotBuyerStable.setBalance(spotBuyerStable.getBalance().add(buyerLockedAmount.subtract(totalCost)));
                }

                // Seller pays Coin (Locked)
                spotSellerCoin.setLockedBalance(spotSellerCoin.getLockedBalance().subtract(quantity));
                break;

            case MARKET_LIMIT_BUY:
                // Buyer is MARKET (Taker), Seller is LIMIT (Maker)
                // Buyer: Balance Stable -> Seller Balance Stable
                // Seller: Locked Coin -> Buyer Balance Coin

                spotBuyerStable.setBalance(spotBuyerStable.getBalance().subtract(totalCost));
                spotSellerCoin.setLockedBalance(spotSellerCoin.getLockedBalance().subtract(quantity));
                break;

            case MARKET_LIMIT_SELL:
                // Buyer is LIMIT (Maker), Seller is MARKET (Taker)
                // Buyer: Locked Stable -> Seller Balance Stable
                // Seller: Balance Coin -> Buyer Balance Coin

                spotBuyerStable.setLockedBalance(spotBuyerStable.getLockedBalance().subtract(totalCost));
                // Refund excess for buyer
                BigDecimal buyerLockedAmount2 = lockedPrice.multiply(quantity);
                if (buyerLockedAmount2.compareTo(totalCost) > 0) {
                    spotBuyerStable.setLockedBalance(
                            spotBuyerStable.getLockedBalance().subtract(buyerLockedAmount2.subtract(totalCost)));
                    spotBuyerStable
                            .setBalance(spotBuyerStable.getBalance().add(buyerLockedAmount2.subtract(totalCost)));
                }

                spotSellerCoin.setBalance(spotSellerCoin.getBalance().subtract(quantity));
                break;

            case MARKET_MARKET:
                // Both MARKET
                // Buyer: Balance Stable -> Seller Balance Stable
                // Seller: Balance Coin -> Buyer Balance Coin

                spotBuyerStable.setBalance(spotBuyerStable.getBalance().subtract(totalCost));
                spotSellerCoin.setBalance(spotSellerCoin.getBalance().subtract(quantity));
                break;
        }

        // Cộng tiền cho người nhận (LUÔN cộng vào balance)
        spotBuyerCoin.setBalance(spotBuyerCoin.getBalance().add(quantity));
        spotSellerStable.setBalance(spotSellerStable.getBalance().add(totalCost));

        // Ghi nhận biến động số dư
        balanceFluctuation(buyerUid, coin, quantity, "Nhận coin từ giao dịch", spotBuyerCoin.getBalance());
        balanceFluctuation(sellerUid, stableCoin, totalCost, "Nhận tiền từ giao dịch", spotSellerStable.getBalance());
        balanceFluctuation(buyerUid, stableCoin, totalCost.negate(), "Trừ tiền mua coin", spotBuyerStable.getBalance());
        balanceFluctuation(sellerUid, coin, quantity.negate(), "Trừ coin bán", spotSellerCoin.getBalance());

        // Lưu tất cả ví
        spotWalletRepository.saveAll(Arrays.asList(
                spotBuyerCoin, spotSellerCoin, spotBuyerStable, spotSellerStable));

        log.info("✅ Trade executed: {} {} @ {} - Buyer: {}, Seller: {}",
                quantity, coin, price, buyerUid, sellerUid);
    }

    @Transactional
    public void balanceFluctuation(String uid, String currency, BigDecimal amount, String type,
            BigDecimal postBalance) {
        SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
        spotWalletHistory.setUserId(uid);
        spotWalletHistory.setAsset(currency);
        spotWalletHistory.setType(type);
        spotWalletHistory.setAmount(amount);
        spotWalletHistory.setBalance(postBalance);
        LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        spotWalletHistory.setCreateDt(createDt);
        spotWalletHistoryRepository.save(spotWalletHistory);
    }

    public Boolean checkBalance(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];

        if (entity.getTradeType().equals(api.exchange.models.OrderBooks.TradeType.MARKET)
                && entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {

            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
            return spotWallet != null && spotWallet.getBalance().compareTo(BigDecimal.ZERO) > 0;
        } else {
            if (entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {
                SpotWallet fundingWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
                if (fundingWallet == null)
                    return false;
                return fundingWallet.getBalance().compareTo(entity.getPrice().multiply(entity.getQuantity())) >= 0;
            } else {
                SpotWallet fundingWallet = spotWalletRepository.findByUidAndCurrency(uid, coin);
                if (fundingWallet == null)
                    return false;
                return fundingWallet.getBalance().compareTo(entity.getQuantity()) >= 0;
            }
        }
    }

    @Transactional
    public void lockBalanceLimit(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];
        if (entity.getTradeType().equals(api.exchange.models.OrderBooks.TradeType.LIMIT)) {
            if (entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrencyWithLock(uid, stable);
                BigDecimal totalStableCoin = entity.getPrice().multiply(entity.getQuantity());
                spotWallet.setLockedBalance(spotWallet.getLockedBalance().add(totalStableCoin));
                spotWallet.setBalance(spotWallet.getBalance().subtract(totalStableCoin));
                spotWalletRepository.save(spotWallet);
            } else {
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrencyWithLock(uid, coin);
                spotWallet.setLockedBalance(spotWallet.getLockedBalance().add(entity.getQuantity()));
                spotWallet.setBalance(spotWallet.getBalance().subtract(entity.getQuantity()));
                spotWalletRepository.save(spotWallet);
            }
        }
    }

    @Transactional
    public void unlockBalance(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];

        if (entity.getTradeType().equals(api.exchange.models.OrderBooks.TradeType.LIMIT)) {
            if (entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {
                // Buyer locked Stable
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrencyWithLock(uid, stable);
                if (spotWallet != null) {

                    BigDecimal remainingQty = entity.getRemainingQuantity() != null ? entity.getRemainingQuantity()
                            : entity.getQuantity();
                    BigDecimal amountToUnlock = remainingQty.multiply(entity.getPrice());

                    spotWallet.setLockedBalance(spotWallet.getLockedBalance().subtract(amountToUnlock));
                    spotWallet.setBalance(spotWallet.getBalance().add(amountToUnlock));
                    spotWalletRepository.save(spotWallet);
                }
            } else {
                // Seller locked Coin
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrencyWithLock(uid, coin);
                if (spotWallet != null) {
                    BigDecimal remainingQty = entity.getRemainingQuantity() != null ? entity.getRemainingQuantity()
                            : entity.getQuantity();

                    spotWallet.setLockedBalance(spotWallet.getLockedBalance().subtract(remainingQty));
                    spotWallet.setBalance(spotWallet.getBalance().add(remainingQty));
                    spotWalletRepository.save(spotWallet);
                }
            }
        }
    }

    @Transactional
    public void checkWalletRecive(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];
        if (entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {
            createWalletIfNotExists(uid, coin);
        } else {
            createWalletIfNotExists(uid, stable);
        }
    }

    private void createWalletIfNotExists(String uid, String currency) {
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null) {
            SpotWallet newWallet = new SpotWallet();
            newWallet.setUid(uid);
            newWallet.setCurrency(currency);
            newWallet.setBalance(BigDecimal.ZERO);
            newWallet.setLockedBalance(BigDecimal.ZERO);
            newWallet.setActive(true);
            spotWalletRepository.save(newWallet);
        }
    }
}
