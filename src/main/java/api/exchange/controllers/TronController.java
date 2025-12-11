package api.exchange.controllers;

import api.exchange.models.TronWallet;
import api.exchange.services.TronTransactionService;
import api.exchange.services.TronWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tron")
public class TronController {

    @Autowired
    private TronWalletService walletService;

    @Autowired
    private TronTransactionService transactionService;

    // Create or retrieve wallet for a user
    // Input: userId (In production, retrieve this from SecurityContext/JWT)
    @PostMapping("/wallet")
    public WalletResponse createWallet(@RequestParam String userId) {
        TronWallet wallet = walletService.createWallet(userId);
        return new WalletResponse(wallet.getUid(), wallet.getAddress());
    }

    // Get balance (Public info)
    @GetMapping("/balance/{address}")
    public long getBalance(@PathVariable String address) {
        return walletService.getBalance(address);
    }

    // Transfer (Custodial)
    // In production, ensure the caller owns the userId via auth checks!
    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {
        if ("TRX".equalsIgnoreCase(request.getType())) {
            return transactionService.sendTrx(request.getUserId(), request.getToAddress(), request.getAmount());
        } else if ("TRC20".equalsIgnoreCase(request.getType())) {
            return transactionService.sendTrc20(request.getUserId(), request.getToAddress(),
                    request.getContractAddress(), request.getAmount());
        } else {
            throw new IllegalArgumentException("Unsupported transfer type: " + request.getType());
        }
    }

    // Get Transaction Info
    @GetMapping("/transaction/{txid}")
    public String getTransaction(@PathVariable String txid) {
        return transactionService.getTransactionById(txid).toString();
    }

    // DTOs
    public static class WalletResponse {
        private String userId;
        private String address;

        public WalletResponse(String userId, String address) {
            this.userId = userId;
            this.address = address;
        }

        public String getUserId() {
            return userId;
        }

        public String getAddress() {
            return address;
        }
    }

    public static class TransferRequest {
        private String type; // TRX or TRC20
        private String userId; // Who is sending
        private String toAddress;
        private String contractAddress; // Only for TRC20
        private long amount;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getToAddress() {
            return toAddress;
        }

        public void setToAddress(String toAddress) {
            this.toAddress = toAddress;
        }

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }
    }
}
