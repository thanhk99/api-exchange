package api.exchange.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.FundingWalletHistory;
import api.exchange.models.SpotWalletHistory;
import api.exchange.models.TransactionEarn;
import api.exchange.repository.FundingWalletHistoryRepository;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.TransactionEarnRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class WalletHistoryService {

    @Autowired
    private FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private TransactionEarnRepository transactionEarnRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Lấy lịch sử giao dịch của ví Funding
     * 
     * @param authHeader JWT token
     * @return Danh sách lịch sử giao dịch Funding Wallet
     */
    public ResponseEntity<?> getFundingHistory(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            List<FundingWalletHistory> history = fundingWalletHistoryRepository.findByUserId(uid);

            List<Map<String, Object>> formattedHistory = history.stream()
                    .map(h -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", h.getId());
                        item.put("asset", h.getAsset());
                        item.put("type", h.getType());
                        item.put("amount", h.getAmount());
                        item.put("balance", h.getBalance());
                        item.put("note", h.getNote() != null ? h.getNote() : "");
                        item.put("createDt", h.getCreateDt());
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", formattedHistory));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi khi lấy lịch sử giao dịch Funding Wallet"));
        }
    }

    /**
     * Lấy lịch sử giao dịch của ví Spot
     * 
     * @param authHeader JWT token
     * @return Danh sách lịch sử giao dịch Spot Wallet
     */
    public ResponseEntity<?> getSpotHistory(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            List<SpotWalletHistory> history = spotWalletHistoryRepository.findByUserId(uid);

            List<Map<String, Object>> formattedHistory = history.stream()
                    .map(h -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", h.getId());
                        item.put("asset", h.getAsset());
                        item.put("type", h.getType());
                        item.put("amount", h.getAmount());
                        item.put("balance", h.getBalance());
                        item.put("note", h.getNote() != null ? h.getNote() : "");
                        item.put("createDt", h.getCreateDt());
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", formattedHistory));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi khi lấy lịch sử giao dịch Spot Wallet"));
        }
    }

    /**
     * Lấy lịch sử giao dịch của ví Earn
     * 
     * @param authHeader JWT token
     * @return Danh sách lịch sử giao dịch Earn Wallet
     */
    public ResponseEntity<?> getEarnHistory(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            List<TransactionEarn> history = transactionEarnRepository.findByUserId(uid);

            List<Map<String, Object>> formattedHistory = history.stream()
                    .map(h -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", h.getId());
                        item.put("asset", h.getAsset());
                        item.put("type", h.getType());
                        item.put("amount", h.getAmount());
                        item.put("balance", h.getBalance());
                        item.put("note", h.getNote() != null ? h.getNote() : "");
                        item.put("createDt", h.getCreateDt());
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", formattedHistory));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi khi lấy lịch sử giao dịch Earn Wallet"));
        }
    }

    /**
     * Lấy tất cả lịch sử giao dịch của tất cả các ví
     * 
     * @param authHeader JWT token
     * @return Danh sách lịch sử giao dịch của tất cả các ví
     */
    public ResponseEntity<?> getAllWalletHistory(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            List<FundingWalletHistory> fundingHistory = fundingWalletHistoryRepository.findByUserId(uid);
            List<SpotWalletHistory> spotHistory = spotWalletHistoryRepository.findByUserId(uid);
            List<TransactionEarn> earnHistory = transactionEarnRepository.findByUserId(uid);

            Map<String, Object> allHistory = new HashMap<>();
            allHistory.put("fundingHistory", fundingHistory);
            allHistory.put("spotHistory", spotHistory);
            allHistory.put("earnHistory", earnHistory);

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", allHistory));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi khi lấy lịch sử giao dịch"));
        }
    }
}
