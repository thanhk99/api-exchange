package api.exchange.controllers;

import api.exchange.models.FundingWallet;
import api.exchange.services.TronTransactionService;
import api.exchange.services.FundingWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import api.exchange.dtos.Request.TronTransferRequest;
import api.exchange.dtos.Response.TronWalletResponse;

@RestController
@RequestMapping("/api/tron")
public class TronController {

    @Autowired
    private FundingWalletService walletService;

    @Autowired
    private TronTransactionService transactionService;

    // Create or retrieve wallet for a user
    @PostMapping("/wallet")
    public TronWalletResponse createWallet(@RequestParam String userId) {
        FundingWallet wallet = walletService.createTronWallet(userId);
        return new TronWalletResponse(wallet.getUid(), wallet.getAddress());
    }

    // Get balance (Public info)
    @GetMapping("/balance/{address}")
    public long getBalance(@PathVariable String address) {
        return walletService.getTronBalance(address);
    }

    // Transfer (Custodial)
    @PostMapping("/transfer")
    public String transfer(@RequestBody TronTransferRequest request) {
        if (!"TRX".equalsIgnoreCase(request.getType())) {
            throw new IllegalArgumentException("Unsupported transfer type: " + request.getType());
        }
        return transactionService.transfer(request.getUserId(), request.getToAddress(), request.getAmount());
    }

    // Estimate Fee Endpoint
    @PostMapping("/estimate-fee")
    public java.math.BigDecimal estimateFee(@RequestBody TronTransferRequest request) {
        if (!"TRX".equalsIgnoreCase(request.getType())) {
            return java.math.BigDecimal.ZERO;
        }
        return transactionService.estimateTrxFee(request.getUserId(), request.getToAddress(), request.getAmount());
    }

    // Get Transaction Info
    @GetMapping("/transaction/{txid}")
    public String getTransaction(@PathVariable String txid) {
        return transactionService.getTransactionById(txid).toString();
    }
}
