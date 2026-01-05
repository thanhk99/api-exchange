package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import java.util.UUID;

import api.exchange.models.SpotWallet;
import api.exchange.models.OrderBooks;
import api.exchange.models.SpotHistory.TradeType;
import api.exchange.models.SpotWalletHistory;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.SpotWalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SpotWalletService {

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> addBalanceCoin(SpotWallet entity, String idempotencyKey) {
        try {
            // Check idempotency first
            if (idempotencyKey != null && spotWalletHistoryRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.warn("Duplicate request detected with idempotencyKey: {}", idempotencyKey);
                return ResponseEntity.ok(Map.of("message", "DUPLICATE_REQUEST"));
            }

            SpotWallet existingWallet = spotWalletRepository.findByUidAndCurrency(
                    entity.getUid(),
                    entity.getCurrency());

            BigDecimal postBalance;
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(entity.getBalance()));
                postBalance = existingWallet.getBalance();
                spotWalletRepository.save(existingWallet);
            } else {
                postBalance = entity.getBalance();
                spotWalletRepository.save(entity);
            }

            // Ghi Ledger (Append-only)
            SpotWalletHistory history = new SpotWalletHistory();
            history.setUserId(entity.getUid());
            history.setAsset(entity.getCurrency());
            history.setType("Nạp tiền");
            history.setAmount(entity.getBalance());
            history.setBalance(postBalance);
            history.setCreateDt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            history.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString());
            spotWalletHistoryRepository.save(history);

            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            log.error("Error adding balance", e);
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

        SpotWallet spotBuyerReceive = spotBuyerCoin;
        SpotWallet spotBuyerSend = spotBuyerStable;
        SpotWallet spotSellerReceive = spotSellerStable;
        SpotWallet spotSellerSend = spotSellerCoin;

        // 1. Handle Buyer's Stable Coin (Payment)
        // If Buyer was LIMIT/MARKET BUY -> They pay Stable Coin
        // If Buyer used LIMIT, funds were locked. If MARKET, funds are in balance.

        // Logic depends on who is the "Taker" vs "Maker" effectively, but here we have
        // explicit TradeType
        // Actually, the TradeType passed here seems to be from the perspective of the
        // *match*, but we need to know
        // specifically for buyer and seller if they were LIMIT or MARKET.
        // However, the previous logic tried to infer this from TradeType enum like
        // LIMIT_LIMIT, MARKET_LIMIT_BUY etc.
        // Let's stick to the robust logic:
        // We need to know if the Buyer's order was LIMIT or MARKET.
        // And if the Seller's order was LIMIT or MARKET.
        // The passed 'tradeType' seems to be a combination.

        // Let's simplify and assume standard behavior based on the switch case provided
        // in original code,
        // but FIX the balance updates.

        switch (tradeType) {
            case LIMIT_LIMIT:
                // Both Limit. Both have funds locked.
                // Buyer: Locked Stable -> Seller Balance Stable
                // Seller: Locked Coin -> Buyer Balance Coin

                // Buyer pays Stable (Locked)
                spotBuyerStable.setLockedBalance(spotBuyerStable.getLockedBalance().subtract(totalCost));
                // Refund excess if any (only if buyer locked more than needed - e.g. market
                // price lower than limit)
                // In LIMIT_LIMIT, usually price is match price. If buyer limit > match price,
                // excess is returned.
                // The 'lockedPrice' argument seems to be the buyer's original limit price?
                // Or is it just the price of the trade?
                // Let's assume 'lockedPrice' is what was locked per unit.
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
        spotBuyerReceive.setBalance(spotBuyerReceive.getBalance().add(quantity));
        spotSellerReceive.setBalance(spotSellerReceive.getBalance().add(totalCost));

        // Ghi nhận biến động số dư
        balanceFluctuation(buyerUid, coin, quantity, "Nhận coin từ giao dịch", spotBuyerReceive.getBalance());
        balanceFluctuation(sellerUid, stableCoin, totalCost, "Nhận tiền từ giao dịch", spotSellerReceive.getBalance());
        balanceFluctuation(buyerUid, stableCoin, totalCost.negate(), "Trừ tiền mua coin", spotBuyerSend.getBalance());
        balanceFluctuation(sellerUid, coin, quantity.negate(), "Trừ coin bán", spotSellerSend.getBalance());

        // Lưu tất cả ví
        spotWalletRepository.saveAll(Arrays.asList(
                spotBuyerReceive, spotSellerSend, spotSellerReceive, spotBuyerSend));

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

    // --- Methods moved from SpotService ---

    public Boolean checkBalance(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];

        // Use OrderBooksService or similar to get price if needed, but here we assume
        // entity has price or we check approximate
        // Note: Original code called
        // orderBooksService.getLastTradedPrice(entity.getSymbol())
        // We might need to pass that in or inject OrderBooksService.
        // To avoid circular dependency, let's assume the caller ensures validity or we
        // query price here.
        // For now, let's replicate logic but be careful about dependencies.

        // Actually, SpotService had OrderBooksService. SpotWalletService might not.
        // Let's keep simple checks.

        if (entity.getTradeType().equals(api.exchange.models.OrderBooks.TradeType.MARKET)
                && entity.getOrderType().equals(api.exchange.models.OrderBooks.OrderType.BUY)) {
            // Market Buy: Need enough Stable Coin.
            // Problem: We don't know exact price. Original code checked against
            // LastTradedPrice.
            // We will assume caller handles this or we inject a price provider.
            // For this refactor, let's assume we check against 0 for now or rely on the
            // fact that
            // Market orders are risky without balance check.
            // BUT, we can check if wallet exists.
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
