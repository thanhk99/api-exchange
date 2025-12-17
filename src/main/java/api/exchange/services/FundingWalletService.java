package api.exchange.services;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // Tron Wallet Logic merged here

    public FundingWallet createTronWallet(String uid) {
        // Check if user already has a TRX wallet
        FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(uid, "TRX");
        if (existingWallet != null && existingWallet.getAddress() != null) {
            return existingWallet;
        }

        // Generate new key pair
        KeyPair keyPair = KeyPair.generate();
        String privateKey = keyPair.toPrivateKey();
        String address = keyPair.toBase58CheckAddress();
        String hexAddress = keyPair.toHexAddress();

        // Encrypt private key
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
        // Simple read-only wrapper
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

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note, String status,
            String address, BigDecimal fee, String hash) {
        try {
            FundingWalletHistory FundingWalletHistory = new FundingWalletHistory();
            FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(
                    fundingWalletRes.getUid(),
                    fundingWalletRes.getCurrency());
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(fundingWalletRes.getBalance()));
                FundingWalletHistory.setBalance(existingWallet.getBalance());
                fundingWalletRepository.save(existingWallet);
            } else {
                fundingWalletRes.setUid(fundingWalletRes.getUid());
                FundingWalletHistory.setBalance(fundingWalletRes.getBalance());
                fundingWalletRepository.save(fundingWalletRes);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            FundingWalletHistory.setUserId(fundingWalletRes.getUid());
            FundingWalletHistory.setAsset(fundingWalletRes.getCurrency());
            FundingWalletHistory.setType("Nạp tiền");
            FundingWalletHistory.setAmount(fundingWalletRes.getBalance());
            FundingWalletHistory.setCreateDt(createDt);
            FundingWalletHistory.setNote(note != null ? note : "");
            FundingWalletHistory.setStatus(status != null ? status : "SUCCESS");
            FundingWalletHistory.setAddress(address);
            FundingWalletHistory.setFee(fee != null ? fee : BigDecimal.ZERO);
            FundingWalletHistory.setHash(hash); // Save Hash
            fundingWalletHistoryRepository.save(FundingWalletHistory);

            // Send SSE Notification
            try {
                api.exchange.models.Notification notification = new api.exchange.models.Notification();
                notification.setUserId(fundingWalletRes.getUid());
                notification.setNotificationTitle("Nạp tiền thành công");
                notification.setNotificationContent(String.format("Bạn đã nhận được %s %s",
                        fundingWalletRes.getBalance().stripTrailingZeros().toPlainString(),
                        fundingWalletRes.getCurrency()));
                notification.setNotificationType(api.exchange.models.Notification.NotificationType.INFO);
                notification.setNotificationType(api.exchange.models.Notification.NotificationType.INFO);

                // Persist notification
                notificationRepository.save(notification);

                // Send via SSE
                sseNotificationService.sendNotification(fundingWalletRes.getUid(), notification);
            } catch (Exception e) {
                log.error("Failed to send SSE notification", e);
            }

            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    // Overload for backward compatibility
    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note, String status,
            String address, BigDecimal fee) {
        return addBalanceCoin(fundingWalletRes, note, status, address, fee, null);
    }

    @Transactional
    public void updateHistory(FundingWalletHistory history) {
        fundingWalletHistoryRepository.save(history);
    }

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String note) {
        return addBalanceCoin(fundingWalletRes, note, "SUCCESS", null, BigDecimal.ZERO, null);
    }

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes) {
        return addBalanceCoin(fundingWalletRes, null);
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
                // Giả sử bạn có một phương thức để lấy tỷ giá quy đổi từ đồng tiền này sang USD
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
        // Đây là một ví dụ giả định. Trong thực tế, bạn sẽ cần tích hợp với một dịch vụ
        // cung cấp tỷ giá hối đoái.
        switch (currency) {
            case "USD":
                return BigDecimal.ONE;
            case "USDT":
                return BigDecimal.ONE; // Giả sử 1 EUR = 1.1 USD
            case "BTC":
                return new BigDecimal("30000"); // Giả sử 1 BTC = 30000 USD
            case "ETH":
                return new BigDecimal("2000"); // Giả sử 1 ETH = 2000 USD
            default:
                return BigDecimal.ZERO; // Nếu không biết tỷ giá, trả về 0
        }
    }
}