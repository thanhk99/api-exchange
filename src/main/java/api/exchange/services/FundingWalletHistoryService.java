package api.exchange.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.FundingWalletHistory;
import api.exchange.repository.FundingWalletHistoryRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class FundingWalletHistoryService {

    @Autowired
    private FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> getListAll(String authHeader) {
        try {
            String token = authHeader.substring(7);
            // Get user information
            String uid = jwtUtil.getUserIdFromToken(token);

            List<FundingWalletHistory> listTxFunding = new ArrayList<>();
            listTxFunding = fundingWalletHistoryRepository.findByUserId(uid);
            return ResponseEntity.ok(Map.of("message", "success", "data", listTxFunding));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }
}
