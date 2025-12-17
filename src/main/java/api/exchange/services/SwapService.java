package api.exchange.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.dtos.Request.SwapRequest;
import api.exchange.models.SpotWallet;
import api.exchange.models.SpotWalletHistory;
import api.exchange.models.coinModel;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.SpotWalletRepository;
import api.exchange.repository.coinRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class SwapService {

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private coinRepository coinRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<?> swapCoin(String authHeader, SwapRequest request) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            // 1. Validate Amount
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số lượng phải lớn hơn 0"));
            }

            // 2. Get Coin Prices
            BigDecimal fromPrice = getPriceInUsdt(request.getFromCoin());
            BigDecimal toPrice = getPriceInUsdt(request.getToCoin());

            if (fromPrice.compareTo(BigDecimal.ZERO) == 0 || toPrice.compareTo(BigDecimal.ZERO) == 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coin không hợp lệ hoặc chưa có giá"));
            }

            // 3. Check Balance (Spot Wallet)
            SpotWallet fromWallet = spotWalletRepository.findByUidAndCurrency(uid, request.getFromCoin());
            if (fromWallet == null || fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số dư không đủ"));
            }

            // 4. Calculate Receive Amount
            // Rate = FromPrice / ToPrice
            // Receive = Amount * Rate
            BigDecimal rate = fromPrice.divide(toPrice, 8, RoundingMode.HALF_DOWN);
            BigDecimal receiveAmount = request.getAmount().multiply(rate);

            // 5. Execute Swap
            // Deduct from source
            fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
            spotWalletRepository.save(fromWallet);

            // Add to destination
            SpotWallet toWallet = spotWalletRepository.findByUidAndCurrency(uid, request.getToCoin());
            if (toWallet == null) {
                toWallet = new SpotWallet();
                toWallet.setUid(uid);
                toWallet.setCurrency(request.getToCoin());
                toWallet.setBalance(BigDecimal.ZERO);
                toWallet.setLockedBalance(BigDecimal.ZERO);
            }
            toWallet.setBalance(toWallet.getBalance().add(receiveAmount));
            spotWalletRepository.save(toWallet);

            // 6. Record History
            recordSpotHistory(uid, request.getFromCoin(), request.getAmount().negate(),
                    "Swap to " + request.getToCoin(), fromWallet.getBalance());
            recordSpotHistory(uid, request.getToCoin(), receiveAmount, "Swap from " + request.getFromCoin(),
                    toWallet.getBalance());

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", Map.of(
                            "fromCoin", request.getFromCoin(),
                            "toCoin", request.getToCoin(),
                            "sentAmount", request.getAmount(),
                            "receivedAmount", receiveAmount,
                            "rate", rate)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Đã xảy ra lỗi không mong muốn"));
        }
    }

    private BigDecimal getPriceInUsdt(String coinSymbol) {
        // Handle USDT case directly
        if (coinSymbol.equalsIgnoreCase("USDT")) {
            return BigDecimal.ONE;
        }

        // Try to find coin by ID (e.g., "BTC")
        coinModel coin = coinRepository.findById(coinSymbol).orElse(null);
        if (coin != null) {
            return coin.getCurrentPrice();
        }

        // If not found, maybe try to find by symbol "BTCUSDT" if the input was "BTC"
        // but stored differently?
        // Based on CoinDataService, ID is "BTC", Symbol is "BTCUSDT".
        // If input is "BTC", findById("BTC") should work.

        return BigDecimal.ZERO;
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
}
