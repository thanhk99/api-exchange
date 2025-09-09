package api.exchange.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.SpotWallet;
import api.exchange.models.SpotWalletHistory;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.SpotWalletRepository;
import jakarta.transaction.Transactional;

@Service
public class SpotWalletService {

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Transactional
    public ResponseEntity<?> addBalanceCoin(SpotWallet entity) {
        try {
            SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
            SpotWallet existingWallet = spotWalletRepository.findByUidAndCurrency(
                    entity.getUid(),
                    entity.getCurrency());
            if (existingWallet != null) {
                existingWallet.setBalance(existingWallet.getBalance().add(entity.getBalance()));
                spotWalletHistory.setBalance(existingWallet.getBalance());
                spotWalletRepository.save(existingWallet);
            } else {
                entity.setUid(entity.getUid());
                spotWalletHistory.setBalance(entity.getBalance());
                spotWalletRepository.save(entity);
            }
            LocalDateTime createDt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            spotWalletHistory.setUserId(entity.getUid());
            spotWalletHistory.setAsset(entity.getCurrency());
            spotWalletHistory.setType("Nạp tiền");
            spotWalletHistory.setAmount(entity.getBalance());
            spotWalletHistory.setCreateDt(createDt);
            spotWalletHistoryRepository.save(spotWalletHistory);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }

    public void saveHistory(SpotWallet request) {

    }
}
