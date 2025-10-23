package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import api.exchange.models.TransactionAds;
import api.exchange.services.TransactionsAdsService;

@RestController
@RequestMapping("api/v1/p2pmarket")
public class TransactionsAdsController {

    @Autowired
    private TransactionsAdsService transactionsAdsService;


    @PostMapping("/placeOrder")
    public ResponseEntity<?> placeOrderTransactions(@RequestBody TransactionAds transactionAds) {
        return transactionsAdsService.createTransaction(transactionAds);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmTransP2P(@RequestBody TransactionAds transactionAds) {
        return transactionsAdsService.releaseCoins(transactionAds.getId());
    }

    @PostMapping("/cancle")
    public ResponseEntity<?> cancleTransBy(@RequestBody TransactionAds transactionAds) {
        return transactionsAdsService.cancleTransP2PBy(transactionAds.getId(), transactionAds.getCancleBy());
    }

}
