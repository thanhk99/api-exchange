package api.exchange.controllers;

import api.exchange.services.FundingWalletHistoryService;
import api.exchange.services.TronDepositScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @Autowired
    private TronDepositScheduler tronDepositScheduler;

    @Autowired
    private FundingWalletHistoryService fundingWalletHistoryService;

    // Get All Transaction History
    @GetMapping
    public ResponseEntity<?> getAllTransactions(@RequestHeader("Authorization") String authHeader) {
        return fundingWalletHistoryService.getListAll(authHeader);
    }

    // Manually Trigger Deposit Scan
    @PostMapping("/scan")
    public ResponseEntity<?> scanTransactions() {
        tronDepositScheduler.checkDeposits();
        return ResponseEntity.ok(Map.of("message", "Transactions update triggered successfully"));
    }
}
