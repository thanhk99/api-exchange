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

@RestController
@RequestMapping("api/v1/funding")
public class FundingWalletController {

    @Autowired
    private FundingWalletService fundingWalletService;

    @PostMapping("addBalance")
    public ResponseEntity<?> addBalance(@RequestBody FundingWallet fundingWallet,
            @RequestHeader("Authorization") String authHeader) {
        return fundingWalletService.addBalanceCoin(fundingWallet, authHeader);
    }
}
