package api.exchange.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.FundingWallet;
import api.exchange.models.SpotHistory.TradeType;
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

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes) {
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
            fundingWalletHistoryRepository.save(FundingWalletHistory);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
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