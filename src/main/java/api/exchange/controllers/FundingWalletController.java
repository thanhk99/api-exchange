package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.FundingWallet;
import api.exchange.services.FundingWalletService;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("api/v1/funding")
public class FundingWalletController {

    @Autowired
    private FundingWalletService fundingWalletService;

    @PostMapping("addBalance")
    public ResponseEntity<?> addBalance(
            @RequestBody FundingWallet fundingWallet,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return fundingWalletService.addBalanceCoin(fundingWallet, null, "SUCCESS", null, java.math.BigDecimal.ZERO,
                null, idempotencyKey);
    }

    @GetMapping("getWallet")
    public ResponseEntity<?> getWallet(@RequestHeader("Authorization") String authHeader) {
        return fundingWalletService.getWalletFunding(authHeader);
    }

    @GetMapping("total")
    public ResponseEntity<?> getTotalMoney(@RequestHeader("Authorization") String authHeader) {
        return fundingWalletService.getTotalMoney(authHeader);
    }
}
