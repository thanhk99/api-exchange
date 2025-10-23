package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.services.FundingWalletHistoryService;

@RestController
@RequestMapping("api/v1/txfunding")
public class TransactionFundingController {

    @Autowired
    private FundingWalletHistoryService transactionFundingService;

    @GetMapping("getAll")
    public ResponseEntity<?> getAll(@RequestHeader("Authorization") String authHeader) {
        return transactionFundingService.getListAll(authHeader);
    }
}
