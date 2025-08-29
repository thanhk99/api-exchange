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
import api.exchange.models.TransactionFunding;
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.TransactionFundingRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FundingWalletService {

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private TransactionFundingRepository transactionFundingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes) {
        try {
            TransactionFunding transactionFunding = new TransactionFunding();
            FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(
                    fundingWalletRes.getUid(),
                    fundingWalletRes.getCurrency());
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(fundingWalletRes.getBalance()));
                transactionFunding.setBalance(existingWallet.getBalance());
                fundingWalletRepository.save(existingWallet);
            } else {
                fundingWalletRes.setUid(fundingWalletRes.getUid());
                transactionFunding.setBalance(fundingWalletRes.getBalance());
                fundingWalletRepository.save(fundingWalletRes);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            transactionFunding.setUserId(fundingWalletRes.getUid());
            transactionFunding.setAsset(fundingWalletRes.getCurrency());
            transactionFunding.setType("Nạp tiền");
            transactionFunding.setAmount(fundingWalletRes.getBalance());
            transactionFunding.setCreateDt(createDt);
            transactionFundingRepository.save(transactionFunding);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    @Transactional
    public void executeTradeSpot(String sellerUid, String buyerUid, BigDecimal price, BigDecimal quantity,
            TradeType tradeType, String symbol , BigDecimal lockedPrice) {
        
        String[] parts = symbol.split("/");
        String coin = parts[0];        
        String stableCoin = parts[1];  

        BigDecimal totalCost = price.multiply(quantity);
        BigDecimal lockedCost  = lockedPrice.multiply(quantity);
        BigDecimal excessAmount = lockedCost.subtract(totalCost); 

        // Lấy ví với pessimistic lock để tránh race condition
        FundingWallet fundingBuyerReceive = fundingWalletRepository.findByUidAndCurrency(buyerUid, coin);
        FundingWallet fundingSellerSend = fundingWalletRepository.findByUidAndCurrency(sellerUid, coin);
        FundingWallet fundingSellerReceive = fundingWalletRepository.findByUidAndCurrency(sellerUid, stableCoin);
        FundingWallet fundingBuyerSend = fundingWalletRepository.findByUidAndCurrency(buyerUid, stableCoin);

        // Kiểm tra số dư
        // if (fundingSellerSend.getAvailableBalance().compareTo(quantity) < 0) {
        //     throw new Exception("Seller insufficient " + coin + " balance");
        // }

        // if (fundingBuyerSend.getAvailableBalance().compareTo(totalCost) < 0) {
        //     throw new Exception("Buyer insufficient " + stableCoin + " balance");
        // }

        // Xử lý theo loại trade
        switch (tradeType) {
            case LIMIT_LIMIT:
                // Trừ từ locked balance
                fundingBuyerSend.setLockedBalance(fundingBuyerSend.getLockedBalance().subtract(totalCost));
                fundingSellerSend.setLockedBalance(fundingSellerSend.getLockedBalance().subtract(quantity));
                if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
                    fundingBuyerSend.setBalance(fundingBuyerSend.getBalance().add(excessAmount));
                }
                break;
                
            case MARKET_LIMIT_BUY:
                // Buyer dùng market (balance), seller dùng limit (locked)
                fundingBuyerSend.setBalance(fundingBuyerSend.getBalance().subtract(totalCost));
                fundingSellerSend.setLockedBalance(fundingSellerSend.getLockedBalance().subtract(quantity));
                break;
                
            case MARKET_LIMIT_SELL:
                // Buyer dùng limit (locked), seller dùng market (balance)
                fundingBuyerSend.setLockedBalance(fundingBuyerSend.getLockedBalance().subtract(totalCost));
                fundingSellerSend.setBalance(fundingSellerSend.getBalance().subtract(quantity));
                break;
                
            case MARKET_MARKET:
                // Cả hai đều dùng balance
                fundingBuyerSend.setBalance(fundingBuyerSend.getBalance().subtract(totalCost));
                fundingSellerSend.setBalance(fundingSellerSend.getBalance().subtract(quantity));
                break;
        }

        // Cộng tiền cho người nhận (LUÔN cộng vào balance)
        fundingBuyerReceive.setBalance(fundingBuyerReceive.getBalance().add(quantity));
        fundingSellerReceive.setBalance(fundingSellerReceive.getBalance().add(totalCost));

        // Lưu tất cả ví
        fundingWalletRepository.saveAll(Arrays.asList(
            fundingBuyerReceive, fundingSellerSend, fundingSellerReceive, fundingBuyerSend
        ));

        log.info("✅ Trade executed: {} {} @ {} - Buyer: {}, Seller: {}", 
                quantity, coin, price, buyerUid, sellerUid);
    }

    public ResponseEntity<?> getWalletFunding(String header){
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        try {
            if(uid == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
            }
            List<FundingWallet> listWallet = fundingWalletRepository.findAllByUid(uid);
            return ResponseEntity.ok(Map.of( "message", "success", "data", listWallet));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    public ResponseEntity<?> getTotalMoney(String header){
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        try {
            if(uid == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
            }
            List<FundingWallet> listWallet = fundingWalletRepository.findAllByUid(uid);
            BigDecimal totalMoney = BigDecimal.ZERO;
            for (FundingWallet wallet : listWallet) {
                // Giả sử bạn có một phương thức để lấy tỷ giá quy đổi từ đồng tiền này sang USD
                BigDecimal exchangeRate = getExchangeRateToUSD(wallet.getCurrency());
                totalMoney = totalMoney.add(wallet.getBalance().multiply(exchangeRate));
            }
            return ResponseEntity.ok(Map.of( "message", "success", "data", totalMoney));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    public BigDecimal getExchangeRateToUSD(String currency) {
        // Đây là một ví dụ giả định. Trong thực tế, bạn sẽ cần tích hợp với một dịch vụ cung cấp tỷ giá hối đoái.
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