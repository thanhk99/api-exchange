package api.exchange.services;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;

import api.exchange.models.FundingWallet;
import api.exchange.models.FundingWalletHistory;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.FundingWalletHistoryRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FundingWalletService {

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private api.exchange.repository.NotificationRepository notificationRepository;

    public FundingWallet createTronWallet(String uid) {
        FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(uid, "TRX");
        if (existingWallet != null && existingWallet.getAddress() != null) {
            return existingWallet;
        }

        KeyPair keyPair = KeyPair.generate();
        String privateKey = keyPair.toPrivateKey();
        String address = keyPair.toBase58CheckAddress();
        String hexAddress = keyPair.toHexAddress();

        String encryptedPrivateKey = encryptionService.encrypt(privateKey);

        if (existingWallet != null) {
            existingWallet.setAddress(address);
            existingWallet.setEncryptedPrivateKey(encryptedPrivateKey);
            existingWallet.setHexAddress(hexAddress);
            return fundingWalletRepository.save(existingWallet);
        } else {
            FundingWallet wallet = new FundingWallet();
            wallet.setUid(uid);
            wallet.setCurrency("TRX");
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setLockedBalance(BigDecimal.ZERO);
            wallet.setAddress(address);
            wallet.setEncryptedPrivateKey(encryptedPrivateKey);
            wallet.setHexAddress(hexAddress);
            return fundingWalletRepository.save(wallet);
        }
    }

    public long getTronBalance(String address) {
        ApiWrapper wrapper = null;
        try {
            wrapper = new ApiWrapper(api.exchange.config.TronConfig.TRON_FULL_NODE,
                    api.exchange.config.TronConfig.TRON_SOLIDITY_NODE,
                    "5c42289c894957e849405d429a888065096a6668740c4a0378b8748383a15286");
            return wrapper.getAccountBalance(address);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching balance for address: " + address, e);
        } finally {
            if (wrapper != null)
                wrapper.close();
        }
    }

    public boolean validateTronAddress(String address) {
        try {
            return address != null && address.startsWith("T") && address.length() == 34;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note, String status,
            String address, BigDecimal fee, String hash, String idempotencyKey) {
        try {
            if (idempotencyKey != null && fundingWalletHistoryRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.warn("Duplicate funding request: {}", idempotencyKey);
                return ResponseEntity.ok(Map.of("message", "DUPLICATE_REQUEST"));
            }

            fundingWalletRepository.advisoryLock(fundingWalletRes.getUid() + "_" + fundingWalletRes.getCurrency());

            FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(
                    fundingWalletRes.getUid(),
                    fundingWalletRes.getCurrency());

            BigDecimal postBalance;
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(fundingWalletRes.getBalance()));
                postBalance = existingWallet.getBalance();
                fundingWalletRepository.save(existingWallet);
            } else {
                postBalance = fundingWalletRes.getBalance();
                fundingWalletRepository.save(fundingWalletRes);
            }

            FundingWalletHistory history = new FundingWalletHistory();
            history.setUserId(fundingWalletRes.getUid());
            history.setAsset(fundingWalletRes.getCurrency());
            history.setType("Nạp tiền");
            history.setAmount(fundingWalletRes.getBalance());
            history.setBalance(postBalance);
            history.setCreateDt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            history.setNote(note != null ? note : "");
            history.setStatus(status != null ? status : "SUCCESS");
            history.setAddress(address);
            history.setFee(fee != null ? fee : BigDecimal.ZERO);
            history.setHash(hash);
            history.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString());
            fundingWalletHistoryRepository.save(history);

            sendTransactionNotification(fundingWalletRes);

            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            log.error("Error processing funding addBalanceCoin", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    private void sendTransactionNotification(FundingWallet wallet) {
        try {
            api.exchange.models.Notification notification = new api.exchange.models.Notification();
            notification.setUserId(wallet.getUid());
            notification.setNotificationTitle("Nạp tiền thành công");
            notification.setNotificationContent(String.format("Bạn đã nhận được %s %s",
                    wallet.getBalance().stripTrailingZeros().toPlainString(),
                    wallet.getCurrency()));
            notification.setNotificationType(api.exchange.models.Notification.NotificationType.INFO);
            notificationRepository.save(notification);
            sseNotificationService.sendNotification(wallet.getUid(), notification);
        } catch (Exception e) {
            log.error("Failed to send SSE notification", e);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note, String status,
            String address, BigDecimal fee, String hash) {
        return addBalanceCoin(fundingWalletRes, note, status, address, fee, hash, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note) {
        return addBalanceCoin(fundingWalletRes, note, "SUCCESS", null, BigDecimal.ZERO, null, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes) {
        return addBalanceCoin(fundingWalletRes, null);
    }

    @Transactional
    public void updateHistory(FundingWalletHistory history) {
        fundingWalletHistoryRepository.save(history);
    }

    public ResponseEntity<?> getWalletFunding(String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);
        try {
            if (uid == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
            }
            List<FundingWallet> listWallet = fundingWalletRepository.findAllByUid(uid);
            return ResponseEntity.ok(Map.of("message", "success", "data", listWallet));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    public ResponseEntity<?> getTotalMoney(String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);
        try {
            if (uid == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
            }
            List<FundingWallet> listWallet = fundingWalletRepository.findAllByUid(uid);
            BigDecimal totalMoney = BigDecimal.ZERO;
            for (FundingWallet wallet : listWallet) {
                BigDecimal exchangeRate = getExchangeRateToUSD(wallet.getCurrency());
                totalMoney = totalMoney.add(wallet.getBalance().multiply(exchangeRate));
            }
            return ResponseEntity.ok(Map.of("message", "success", "data", totalMoney));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    public BigDecimal getExchangeRateToUSD(String currency) {
        switch (currency) {
            case "USD":
            case "USDT":
                return BigDecimal.ONE;
            case "BTC":
                return new BigDecimal("30000");
            case "ETH":
                return new BigDecimal("2000");
            default:
                return BigDecimal.ZERO;
        }
    }
}