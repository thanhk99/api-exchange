package api.exchange.services;

import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.FundingWallet;
import api.exchange.models.TransactionFunding;
import api.exchange.repository.FundingWalletRepository;
import api.exchange.repository.TransactionFundingRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class FundingWalletService {

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private TransactionFundingRepository transactionFundingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<?> addBalanceCoin(FundingWallet fundingWalletRes, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);
            TransactionFunding transactionFunding = new TransactionFunding();
            FundingWallet existingWallet = fundingWalletRepository.findByUidAndCurrency(
                    uid,
                    fundingWalletRes.getCurrency());
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(fundingWalletRes.getBalance()));
                transactionFunding.setBalance(existingWallet.getBalance());
                fundingWalletRepository.save(existingWallet);
            } else {
                fundingWalletRes.setUid(uid);
                transactionFunding.setBalance(fundingWalletRes.getBalance());
                fundingWalletRepository.save(fundingWalletRes);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            transactionFunding.setUserId(uid);
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

}