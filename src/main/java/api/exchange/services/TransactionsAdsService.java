package api.exchange.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.TransactionAds;
import api.exchange.repository.TransactionsAdsRepository;

@Service
public class TransactionsAdsService {
    @Autowired
    private TransactionsAdsRepository transactionsAdsRepository;

    @Autowired
    private P2PADService p2padService;

    @Transactional
    public ResponseEntity<?> placeOrderTransactions(TransactionAds transactionAds) {
        try {
            p2padService.lockCoinP2P(transactionAds);
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            transactionAds.setCreatedAt(createDt);
            transactionsAdsRepository.save(transactionAds);
            return ResponseEntity.ok(Map.of("message", "success", "data", transactionAds));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "SERVER_ERROR"));
        }
    }

    @Transactional
    public ResponseEntity<?> confirmTransP2P(Long idTransaction) {
        try {
            TransactionAds transactionAds = transactionsAdsRepository.getById(idTransaction);
            transactionAds.setStatus("COMPLETED");
            transactionsAdsRepository.save(transactionAds);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "SERVER_ERROR"));
        }
    }

    @Transactional
    public ResponseEntity<?> cancleTransP2PBy(Long idTransaction) {
        try {
            TransactionAds transactionAds = transactionsAdsRepository.getById(idTransaction);
            transactionAds.setStatus("COMPLETED");
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "SERVER_ERROR"));
        }
    }
}
