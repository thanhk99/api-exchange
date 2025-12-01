package api.exchange.services;

import api.exchange.models.FuturesTransaction;
import api.exchange.models.FuturesWallet;
import api.exchange.models.SpotWallet;
import api.exchange.repository.FuturesTransactionRepository;
import api.exchange.repository.FuturesWalletRepository;
import api.exchange.repository.SpotWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class FuturesWalletService {

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private FuturesTransactionRepository futuresTransactionRepository;

    @Autowired
    private SpotWalletRepository spotWalletRepository;

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
