package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.services.WalletHistoryService;

@RestController
@RequestMapping("/api/v1/wallet-history")
public class WalletHistoryController {

    @Autowired
    private WalletHistoryService walletHistoryService;

    @GetMapping("/funding")
    public ResponseEntity<?> getFundingHistory(@RequestHeader("Authorization") String authHeader) {
        return walletHistoryService.getFundingHistory(authHeader);
    }

    @GetMapping("/spot")
    public ResponseEntity<?> getSpotHistory(@RequestHeader("Authorization") String authHeader) {
        return walletHistoryService.getSpotHistory(authHeader);
    }

    @GetMapping("/earn")
    public ResponseEntity<?> getEarnHistory(@RequestHeader("Authorization") String authHeader) {
        return walletHistoryService.getEarnHistory(authHeader);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllWalletHistory(@RequestHeader("Authorization") String authHeader) {
        return walletHistoryService.getAllWalletHistory(authHeader);
    }
}
