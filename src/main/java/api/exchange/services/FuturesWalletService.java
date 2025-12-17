package api.exchange.services;

import api.exchange.dtos.Response.FuturesWalletResponse;
import api.exchange.models.FuturesPosition;
import api.exchange.models.FuturesTransaction;
import api.exchange.models.FuturesWallet;
import api.exchange.models.SpotWallet;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.FuturesTransactionRepository;
import api.exchange.repository.FuturesWalletRepository;
import api.exchange.repository.SpotWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FuturesWalletService {

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private FuturesTransactionRepository futuresTransactionRepository;

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private CoinDataService coinDataService;

    public FuturesWallet getWallet(String uid, String currency) {
        return futuresWalletRepository.findByUidAndCurrency(uid, currency)
                .orElseGet(() -> {
                    FuturesWallet wallet = new FuturesWallet();
                    wallet.setUid(uid);
                    wallet.setCurrency(currency);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setLockedBalance(BigDecimal.ZERO);
                    return futuresWalletRepository.save(wallet);
                });
    }

    public FuturesWalletResponse getWalletInfo(String uid, String currency) {
        FuturesWallet wallet = getWallet(uid, currency);

        // Get open positions
        List<FuturesPosition> openPositions = futuresPositionRepository
                .findByUidAndStatus(uid, FuturesPosition.PositionStatus.OPEN);

        // Calculate available balance
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());

        // Calculate unrealized PnL and total position value
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalPositionValue = BigDecimal.ZERO;

        for (FuturesPosition position : openPositions) {
            BigDecimal currentPrice = coinDataService.getCurrentPrice(position.getSymbol());
            if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                // Calculate PnL
                BigDecimal pnl;
                if (position.getSide() == FuturesPosition.PositionSide.LONG) {
                    pnl = currentPrice.subtract(position.getEntryPrice())
                            .multiply(position.getQuantity());
                } else {
                    pnl = position.getEntryPrice().subtract(currentPrice)
                            .multiply(position.getQuantity());
                }
                unrealizedPnl = unrealizedPnl.add(pnl);

                // Calculate position value
                BigDecimal positionValue = currentPrice.multiply(position.getQuantity());
                totalPositionValue = totalPositionValue.add(positionValue);
            }
        }

        // Calculate margin ratio
        BigDecimal marginRatio = BigDecimal.ZERO;
        if (wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            marginRatio = wallet.getLockedBalance()
                    .divide(wallet.getBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new FuturesWalletResponse(
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getLockedBalance(),
                availableBalance,
                unrealizedPnl,
                totalPositionValue,
                marginRatio,
                openPositions.size());
    }

    @Transactional
    public void transferToFutures(String uid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // 1. Deduct from Spot
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, "USDT");
        if (spotWallet == null) {
            throw new RuntimeException("Spot wallet not found");
        }

        if (spotWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient spot balance");
        }

        spotWallet.setBalance(spotWallet.getBalance().subtract(amount));
        spotWalletRepository.save(spotWallet);

        // 2. Add to Futures
        FuturesWallet futuresWallet = getWallet(uid, "USDT");
        futuresWallet.setBalance(futuresWallet.getBalance().add(amount));
        futuresWalletRepository.save(futuresWallet);

        // 3. Record Transactions
        FuturesTransaction tx = new FuturesTransaction();
        tx.setUid(uid);
        tx.setType(FuturesTransaction.TransactionType.TRANSFER_IN);
        tx.setAmount(amount);
        tx.setCurrency("USDT");
        tx.setCreatedAt(LocalDateTime.now());
        futuresTransactionRepository.save(tx);
    }

    @Transactional
    public void transferFromFutures(String uid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // 1. Deduct from Futures
        FuturesWallet futuresWallet = getWallet(uid, "USDT");
        BigDecimal availableBalance = futuresWallet.getBalance().subtract(futuresWallet.getLockedBalance());

        if (availableBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient futures balance");
        }

        futuresWallet.setBalance(futuresWallet.getBalance().subtract(amount));
        futuresWalletRepository.save(futuresWallet);

        // 2. Add to Spot
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, "USDT");
        if (spotWallet == null) {
            // Create if not exists (should rarely happen for USDT but good for safety)
            throw new RuntimeException("Spot wallet not found");
        }

        spotWallet.setBalance(spotWallet.getBalance().add(amount));
        spotWalletRepository.save(spotWallet);

        // 3. Record Transaction
        FuturesTransaction tx = new FuturesTransaction();
        tx.setUid(uid);
        tx.setType(FuturesTransaction.TransactionType.TRANSFER_OUT);
        tx.setAmount(amount);
        tx.setCurrency("USDT");
        tx.setCreatedAt(LocalDateTime.now());
        futuresTransactionRepository.save(tx);
    }
}
