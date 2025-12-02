package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.dtos.Request.InternalTransferRequest;
import api.exchange.dtos.Request.WalletTransferRequest;
import api.exchange.models.EarnWallet;
import api.exchange.models.FundingWallet;
import api.exchange.models.FundingWalletHistory;
import api.exchange.models.SpotWallet;
import api.exchange.models.SpotWalletHistory;
import api.exchange.models.TransactionEarn;
import api.exchange.models.User;
import api.exchange.repository.EarnWalletRepository;
import api.exchange.repository.FundingWalletHistoryRepository;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.SpotWalletRepository;
import api.exchange.repository.TransactionEarnRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.models.FuturesTransaction;
import api.exchange.models.FuturesWallet;
import api.exchange.repository.FuturesTransactionRepository;
import api.exchange.repository.FuturesWalletRepository;

@Service
public class TransferService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EarnWalletRepository earnWalletRepository;

    @Autowired
    private TransactionEarnRepository transactionEarnRepository;

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private FuturesTransactionRepository futuresTransactionRepository;

    @Transactional
    public ResponseEntity<?> internalTransfer(String authHeader, InternalTransferRequest request) {
        try {
            // 1. Validate User
            String token = authHeader.substring(7);
            String senderUid = jwtUtil.getUserIdFromToken(token);
            User sender = userRepository.findByUid(senderUid);

            if (sender == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Sender not found"));
            }

            // 2. Find Recipient
            User recipient = findRecipient(request.getRecipientIdentifier());
            if (recipient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Recipient not found"));
            }

            if (sender.getUid().equals(recipient.getUid())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot transfer to yourself"));
            }

            // 3. Validate Amount
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Amount must be positive"));
            }

            // 4. Execute Transfer (Using Funding Wallet)
            FundingWallet senderWallet = fundingWalletRepository.findByUidAndCurrency(senderUid, request.getCurrency());
            if (senderWallet == null || senderWallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Insufficient balance"));
            }

            FundingWallet recipientWallet = fundingWalletRepository.findByUidAndCurrency(recipient.getUid(),
                    request.getCurrency());
            if (recipientWallet == null) {
                // Create wallet if not exists
                recipientWallet = new FundingWallet();
                recipientWallet.setUid(recipient.getUid());
                recipientWallet.setCurrency(request.getCurrency());
                recipientWallet.setBalance(BigDecimal.ZERO);
                recipientWallet.setLockedBalance(BigDecimal.ZERO);
                fundingWalletRepository.save(recipientWallet);
            }

            // Deduct from sender
            senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
            fundingWalletRepository.save(senderWallet);

            // Add to recipient
            recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));
            fundingWalletRepository.save(recipientWallet);

            // 5. Record History
            recordFundingHistory(senderUid, request.getCurrency(), request.getAmount().negate(),
                    "Internal Transfer Sent to " + recipient.getEmail(), senderWallet.getBalance());
            recordFundingHistory(recipient.getUid(), request.getCurrency(), request.getAmount(),
                    "Internal Transfer Received from " + sender.getEmail(), recipientWallet.getBalance());

            return ResponseEntity.ok(Map.of("message", "success", "data", "Transfer successful"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred"));
        }
    }

    @Transactional
    public ResponseEntity<?> walletTransfer(String authHeader, WalletTransferRequest request) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Amount must be positive"));
            }

            String from = request.getFromWallet().toUpperCase();
            String to = request.getToWallet().toUpperCase();

            if (from.equals("FUNDING") && to.equals("SPOT")) {
                return transferFundingToSpot(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("SPOT") && to.equals("FUNDING")) {
                return transferSpotToFunding(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("FUNDING") && to.equals("EARN")) {
                return transferFundingToEarn(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("EARN") && to.equals("FUNDING")) {
                return transferEarnToFunding(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("SPOT") && to.equals("EARN")) {
                return transferSpotToEarn(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("EARN") && to.equals("SPOT")) {
                return transferEarnToSpot(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("SPOT") && to.equals("FUTURES")) {
                return transferSpotToFutures(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("FUTURES") && to.equals("SPOT")) {
                return transferFuturesToSpot(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("FUNDING") && to.equals("FUTURES")) {
                return transferFundingToFutures(uid, request.getCurrency(), request.getAmount());
            } else if (from.equals("FUTURES") && to.equals("FUNDING")) {
                return transferFuturesToFunding(uid, request.getCurrency(), request.getAmount());
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid wallet types"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred"));
        }
    }

    private ResponseEntity<?> transferFundingToSpot(String uid, String currency, BigDecimal amount) {
        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null || fundingWallet.getAvailableBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient funding balance"));
        }

        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null) {
            spotWallet = new SpotWallet();
            spotWallet.setUid(uid);
            spotWallet.setCurrency(currency);
            spotWallet.setBalance(BigDecimal.ZERO);
            spotWallet.setLockedBalance(BigDecimal.ZERO);
            spotWalletRepository.save(spotWallet);
        }

        fundingWallet.setBalance(fundingWallet.getBalance().subtract(amount));
        spotWallet.setBalance(spotWallet.getBalance().add(amount));

        fundingWalletRepository.save(fundingWallet);
        spotWalletRepository.save(spotWallet);

        recordFundingHistory(uid, currency, amount.negate(), "Transfer to Spot Wallet", fundingWallet.getBalance());
        recordSpotHistory(uid, currency, amount, "Transfer from Funding Wallet", spotWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferSpotToFunding(String uid, String currency, BigDecimal amount) {
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null
                || spotWallet.getBalance().subtract(spotWallet.getLockedBalance()).compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient spot balance"));
        }

        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null) {
            fundingWallet = new FundingWallet();
            fundingWallet.setUid(uid);
            fundingWallet.setCurrency(currency);
            fundingWallet.setBalance(BigDecimal.ZERO);
            fundingWallet.setLockedBalance(BigDecimal.ZERO);
            fundingWalletRepository.save(fundingWallet);
        }

        spotWallet.setBalance(spotWallet.getBalance().subtract(amount));
        fundingWallet.setBalance(fundingWallet.getBalance().add(amount));

        spotWalletRepository.save(spotWallet);
        fundingWalletRepository.save(fundingWallet);

        recordSpotHistory(uid, currency, amount.negate(), "Transfer to Funding Wallet", spotWallet.getBalance());
        recordFundingHistory(uid, currency, amount, "Transfer from Spot Wallet", fundingWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferFundingToEarn(String uid, String currency, BigDecimal amount) {
        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null || fundingWallet.getAvailableBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient funding balance"));
        }

        EarnWallet earnWallet = earnWalletRepository.findByUidAndCurrency(uid, currency);
        if (earnWallet == null) {
            earnWallet = new EarnWallet();
            earnWallet.setUid(uid);
            earnWallet.setCurrency(currency);
            earnWallet.setTotalBalance(BigDecimal.ZERO);
            earnWallet.setAvailableBalance(BigDecimal.ZERO);
            earnWallet.setLockedBalance(BigDecimal.ZERO);
            earnWalletRepository.save(earnWallet);
        }

        fundingWallet.setBalance(fundingWallet.getBalance().subtract(amount));
        earnWallet.setAvailableBalance(earnWallet.getAvailableBalance().add(amount));
        earnWallet.setTotalBalance(earnWallet.getTotalBalance().add(amount));

        fundingWalletRepository.save(fundingWallet);
        earnWalletRepository.save(earnWallet);

        recordFundingHistory(uid, currency, amount.negate(), "Transfer to Earn Wallet", fundingWallet.getBalance());
        recordEarnHistory(uid, currency, amount, "Transfer from Funding Wallet", earnWallet.getTotalBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferEarnToFunding(String uid, String currency, BigDecimal amount) {
        EarnWallet earnWallet = earnWalletRepository.findByUidAndCurrency(uid, currency);
        if (earnWallet == null || earnWallet.getAvailableBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient earn balance"));
        }

        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null) {
            fundingWallet = new FundingWallet();
            fundingWallet.setUid(uid);
            fundingWallet.setCurrency(currency);
            fundingWallet.setBalance(BigDecimal.ZERO);
            fundingWallet.setLockedBalance(BigDecimal.ZERO);
            fundingWalletRepository.save(fundingWallet);
        }

        earnWallet.setAvailableBalance(earnWallet.getAvailableBalance().subtract(amount));
        earnWallet.setTotalBalance(earnWallet.getTotalBalance().subtract(amount));
        fundingWallet.setBalance(fundingWallet.getBalance().add(amount));

        earnWalletRepository.save(earnWallet);
        fundingWalletRepository.save(fundingWallet);

        recordEarnHistory(uid, currency, amount.negate(), "Transfer to Funding Wallet", earnWallet.getTotalBalance());
        recordFundingHistory(uid, currency, amount, "Transfer from Earn Wallet", fundingWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferSpotToEarn(String uid, String currency, BigDecimal amount) {
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null
                || spotWallet.getBalance().subtract(spotWallet.getLockedBalance()).compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient spot balance"));
        }

        EarnWallet earnWallet = earnWalletRepository.findByUidAndCurrency(uid, currency);
        if (earnWallet == null) {
            earnWallet = new EarnWallet();
            earnWallet.setUid(uid);
            earnWallet.setCurrency(currency);
            earnWallet.setTotalBalance(BigDecimal.ZERO);
            earnWallet.setAvailableBalance(BigDecimal.ZERO);
            earnWallet.setLockedBalance(BigDecimal.ZERO);
            earnWalletRepository.save(earnWallet);
        }

        spotWallet.setBalance(spotWallet.getBalance().subtract(amount));
        earnWallet.setAvailableBalance(earnWallet.getAvailableBalance().add(amount));
        earnWallet.setTotalBalance(earnWallet.getTotalBalance().add(amount));

        spotWalletRepository.save(spotWallet);
        earnWalletRepository.save(earnWallet);

        recordSpotHistory(uid, currency, amount.negate(), "Transfer to Earn Wallet", spotWallet.getBalance());
        recordEarnHistory(uid, currency, amount, "Transfer from Spot Wallet", earnWallet.getTotalBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferEarnToSpot(String uid, String currency, BigDecimal amount) {
        EarnWallet earnWallet = earnWalletRepository.findByUidAndCurrency(uid, currency);
        if (earnWallet == null || earnWallet.getAvailableBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient earn balance"));
        }

        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null) {
            spotWallet = new SpotWallet();
            spotWallet.setUid(uid);
            spotWallet.setCurrency(currency);
            spotWallet.setBalance(BigDecimal.ZERO);
            spotWallet.setLockedBalance(BigDecimal.ZERO);
            spotWalletRepository.save(spotWallet);
        }

        earnWallet.setAvailableBalance(earnWallet.getAvailableBalance().subtract(amount));
        earnWallet.setTotalBalance(earnWallet.getTotalBalance().subtract(amount));
        spotWallet.setBalance(spotWallet.getBalance().add(amount));

        earnWalletRepository.save(earnWallet);
        spotWalletRepository.save(spotWallet);

        recordEarnHistory(uid, currency, amount.negate(), "Transfer to Spot Wallet", earnWallet.getTotalBalance());
        recordSpotHistory(uid, currency, amount, "Transfer from Earn Wallet", spotWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private User findRecipient(String identifier) {
        User recipient = userRepository.findByUid(identifier);
        if (recipient == null)
            recipient = userRepository.findByEmail(identifier);
        if (recipient == null)
            recipient = userRepository.findByPhone(identifier);
        return recipient;
    }

    private void recordFundingHistory(String uid, String asset, BigDecimal amount, String type,
            BigDecimal postBalance) {
        FundingWalletHistory history = new FundingWalletHistory();
        history.setUserId(uid);
        history.setAsset(asset);
        history.setAmount(amount);
        history.setBalance(postBalance);
        history.setType(type);
        history.setCreateDt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        fundingWalletHistoryRepository.save(history);
    }

    private void recordSpotHistory(String uid, String asset, BigDecimal amount, String type, BigDecimal postBalance) {
        SpotWalletHistory history = new SpotWalletHistory();
        history.setUserId(uid);
        history.setAsset(asset);
        history.setAmount(amount);
        history.setBalance(postBalance);
        history.setType(type);
        history.setCreateDt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        spotWalletHistoryRepository.save(history);
    }

    private void recordEarnHistory(String uid, String asset, BigDecimal amount, String type, BigDecimal postBalance) {
        TransactionEarn history = new TransactionEarn();
        history.setUserId(uid);
        history.setAsset(asset);
        history.setAmount(amount);
        history.setBalance(postBalance);
        history.setType(type);
        history.setCreateDt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        transactionEarnRepository.save(history);
    }

    private ResponseEntity<?> transferSpotToFutures(String uid, String currency, BigDecimal amount) {
        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null
                || spotWallet.getBalance().subtract(spotWallet.getLockedBalance()).compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient spot balance"));
        }

        FuturesWallet futuresWallet = futuresWalletRepository.findByUidAndCurrency(uid, currency)
                .orElseGet(() -> {
                    FuturesWallet wallet = new FuturesWallet();
                    wallet.setUid(uid);
                    wallet.setCurrency(currency);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setLockedBalance(BigDecimal.ZERO);
                    return futuresWalletRepository.save(wallet);
                });

        spotWallet.setBalance(spotWallet.getBalance().subtract(amount));
        futuresWallet.setBalance(futuresWallet.getBalance().add(amount));

        spotWalletRepository.save(spotWallet);
        futuresWalletRepository.save(futuresWallet);

        recordSpotHistory(uid, currency, amount.negate(), "Transfer to Futures Wallet", spotWallet.getBalance());
        recordFuturesTransaction(uid, currency, amount, FuturesTransaction.TransactionType.TRANSFER_IN);

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferFuturesToSpot(String uid, String currency, BigDecimal amount) {
        FuturesWallet futuresWallet = futuresWalletRepository.findByUidAndCurrency(uid, currency)
                .orElse(null);

        if (futuresWallet == null
                || futuresWallet.getBalance().subtract(futuresWallet.getLockedBalance()).compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient futures balance"));
        }

        SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, currency);
        if (spotWallet == null) {
            spotWallet = new SpotWallet();
            spotWallet.setUid(uid);
            spotWallet.setCurrency(currency);
            spotWallet.setBalance(BigDecimal.ZERO);
            spotWallet.setLockedBalance(BigDecimal.ZERO);
            spotWalletRepository.save(spotWallet);
        }

        futuresWallet.setBalance(futuresWallet.getBalance().subtract(amount));
        spotWallet.setBalance(spotWallet.getBalance().add(amount));

        futuresWalletRepository.save(futuresWallet);
        spotWalletRepository.save(spotWallet);

        recordFuturesTransaction(uid, currency, amount, FuturesTransaction.TransactionType.TRANSFER_OUT);
        recordSpotHistory(uid, currency, amount, "Transfer from Futures Wallet", spotWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferFundingToFutures(String uid, String currency, BigDecimal amount) {
        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null || fundingWallet.getAvailableBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient funding balance"));
        }

        FuturesWallet futuresWallet = futuresWalletRepository.findByUidAndCurrency(uid, currency)
                .orElseGet(() -> {
                    FuturesWallet wallet = new FuturesWallet();
                    wallet.setUid(uid);
                    wallet.setCurrency(currency);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setLockedBalance(BigDecimal.ZERO);
                    return futuresWalletRepository.save(wallet);
                });

        fundingWallet.setBalance(fundingWallet.getBalance().subtract(amount));
        futuresWallet.setBalance(futuresWallet.getBalance().add(amount));

        fundingWalletRepository.save(fundingWallet);
        futuresWalletRepository.save(futuresWallet);

        recordFundingHistory(uid, currency, amount.negate(), "Transfer to Futures Wallet", fundingWallet.getBalance());
        recordFuturesTransaction(uid, currency, amount, FuturesTransaction.TransactionType.TRANSFER_IN);

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private ResponseEntity<?> transferFuturesToFunding(String uid, String currency, BigDecimal amount) {
        FuturesWallet futuresWallet = futuresWalletRepository.findByUidAndCurrency(uid, currency)
                .orElse(null);

        if (futuresWallet == null
                || futuresWallet.getBalance().subtract(futuresWallet.getLockedBalance()).compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient futures balance"));
        }

        FundingWallet fundingWallet = fundingWalletRepository.findByUidAndCurrency(uid, currency);
        if (fundingWallet == null) {
            fundingWallet = new FundingWallet();
            fundingWallet.setUid(uid);
            fundingWallet.setCurrency(currency);
            fundingWallet.setBalance(BigDecimal.ZERO);
            fundingWallet.setLockedBalance(BigDecimal.ZERO);
            fundingWalletRepository.save(fundingWallet);
        }

        futuresWallet.setBalance(futuresWallet.getBalance().subtract(amount));
        fundingWallet.setBalance(fundingWallet.getBalance().add(amount));

        futuresWalletRepository.save(futuresWallet);
        fundingWalletRepository.save(fundingWallet);

        recordFuturesTransaction(uid, currency, amount, FuturesTransaction.TransactionType.TRANSFER_OUT);
        recordFundingHistory(uid, currency, amount, "Transfer from Futures Wallet", fundingWallet.getBalance());

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private void recordFuturesTransaction(String uid, String currency, BigDecimal amount,
            FuturesTransaction.TransactionType type) {
        FuturesTransaction tx = new FuturesTransaction();
        tx.setUid(uid);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setCreatedAt(LocalDateTime.now());
        futuresTransactionRepository.save(tx);
    }
}
