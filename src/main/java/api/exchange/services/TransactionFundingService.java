package api.exchange.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.TransactionFunding;
import api.exchange.repository.TransactionFundingRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class TransactionFundingService {

    @Autowired
    private TransactionFundingRepository transactionFundingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> getListAll(String authHeader) {
        try {
            String token = authHeader.substring(7);
            // Get user information
            String uid = jwtUtil.getUserIdFromToken(token);

            List<TransactionFunding> listTxFunding = new ArrayList<>();
            listTxFunding = transactionFundingRepository.findByUserId(uid);
            return ResponseEntity.ok(Map.of("message", "success", "data", listTxFunding));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }
}
