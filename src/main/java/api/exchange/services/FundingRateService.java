package api.exchange.services;

import api.exchange.models.*;
import api.exchange.repository.FuturesFundingRateRepository;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.FuturesTransactionRepository;
import api.exchange.repository.FuturesWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FundingRateService {

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private FuturesTransactionRepository futuresTransactionRepository;

    @Autowired
    private FuturesFundingRateRepository futuresFundingRateRepository;

    @Autowired
    private CoinDataService coinDataService;

    // Mock Funding Rate for MVP (In real app, fetch from Binance/Oracle)
    private static final BigDecimal DEFAULT_FUNDING_RATE = new BigDecimal("0.0001"); // 0.01%

    @Scheduled(cron = "0 0 0,8,16 * * *") // Run at 00:00, 08:00, 16:00
    @Transactional
    public void applyFundingFees() {
        System.out.println("⏳ Applying Funding Fees...");

        // For MVP, we assume a constant positive funding rate for all symbols
        // Real logic: Fetch rate per symbol

        List<FuturesPosition> openPositions = futuresPositionRepository.findAll(); // Should filter by status=OPEN

        for (FuturesPosition position : openPositions) {
            if (position.getStatus() != FuturesPosition.PositionStatus.OPEN)
                continue;

            BigDecimal currentPrice = coinDataService.getCurrentPrice(position.getSymbol());
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0)
                continue;

            // Funding Fee = Position Value * Funding Rate
            BigDecimal positionValue = position.getQuantity().multiply(currentPrice);
            BigDecimal fundingFee = positionValue.multiply(DEFAULT_FUNDING_RATE);

            FuturesWallet wallet = futuresWalletRepository.findByUidAndCurrency(position.getUid(), "USDT")
                    .orElse(null);

            if (wallet == null)
                continue;

            // If Rate > 0: Long pays Short
            // If Rate < 0: Short pays Long

            BigDecimal amountToDeduct = BigDecimal.ZERO;
            BigDecimal amountToAdd = BigDecimal.ZERO;

            if (DEFAULT_FUNDING_RATE.compareTo(BigDecimal.ZERO) > 0) {
                if (position.getSide() == FuturesPosition.PositionSide.LONG) {
                    amountToDeduct = fundingFee;
                } else {
                    amountToAdd = fundingFee;
                }
            } else {
                if (position.getSide() == FuturesPosition.PositionSide.SHORT) {
                    amountToDeduct = fundingFee.abs();
                } else {
                    amountToAdd = fundingFee.abs();
                }
            }

            if (amountToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                // Deduct from Wallet Balance (not Margin)
                wallet.setBalance(wallet.getBalance().subtract(amountToDeduct));

                // Record Transaction
                recordTransaction(position.getUid(), amountToDeduct.negate(), "FUNDING_FEE_PAID", position.getId());
            }

            if (amountToAdd.compareTo(BigDecimal.ZERO) > 0) {
                wallet.setBalance(wallet.getBalance().add(amountToAdd));

                // Record Transaction
                recordTransaction(position.getUid(), amountToAdd, "FUNDING_FEE_RECEIVED", position.getId());
            }

            futuresWalletRepository.save(wallet);
        }

        // Save Rate History (Mock)
        FuturesFundingRate rateHistory = new FuturesFundingRate();
        rateHistory.setSymbol("ALL");
        rateHistory.setRate(DEFAULT_FUNDING_RATE);
        futuresFundingRateRepository.save(rateHistory);

        System.out.println("✅ Funding Fees Applied.");
    }

    private void recordTransaction(String uid, BigDecimal amount, String typeStr, Long positionId) {
        FuturesTransaction tx = new FuturesTransaction();
        tx.setUid(uid);
        tx.setType(FuturesTransaction.TransactionType.FUNDING_FEE);
        tx.setAmount(amount);
        tx.setCurrency("USDT");
        tx.setReferenceId("POS-" + positionId);
        futuresTransactionRepository.save(tx);
    }
}
