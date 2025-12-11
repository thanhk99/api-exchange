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
    @Autowired
    private api.exchange.repository.FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private api.exchange.repository.FundingWalletRepository fundingWalletRepository;

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {
        if (!"TRX".equalsIgnoreCase(request.getType())) {
            // Pending TRC20 balance logic implementation
            throw new IllegalArgumentException("Unsupported transfer type for auto-deduction: " + request.getType());
        }

        java.math.BigDecimal amountTrx = java.math.BigDecimal.valueOf(request.getAmount());
        api.exchange.models.FundingWallet wallet = fundingWalletRepository.findByUidAndCurrency(request.getUserId(),
                "TRX");

        if (wallet == null || wallet.getBalance().compareTo(amountTrx) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // 1. Deduct Balance
        wallet.setBalance(wallet.getBalance().subtract(amountTrx));
        fundingWalletRepository.save(wallet);

        String txid;
        try {
            // 2. Send Transaction
            if ("TRX".equalsIgnoreCase(request.getType())) {
                txid = transactionService.sendTrx(request.getUserId(), request.getToAddress(), request.getAmount());
            } else {
                // Should not happen due to check above, but for completeness
                throw new IllegalArgumentException("Unsupported type");
            }

            // 3. Record Successful History
            api.exchange.models.FundingWalletHistory history = new api.exchange.models.FundingWalletHistory();
            history.setUserId(request.getUserId());
            history.setAsset("TRX");
            history.setAmount(amountTrx.negate());
            history.setBalance(wallet.getBalance());
            history.setType("Rút tiền");
            history.setCreateDt(java.time.LocalDateTime.now());
            history.setNote("Withdraw to " + request.getToAddress() + " | TxID: " + txid);
            history.setStatus("SUCCESS");
            history.setAddress(request.getToAddress());
            history.setFee(java.math.BigDecimal.ZERO);
            fundingWalletHistoryRepository.save(history);

        } catch (Exception e) {
            // 4. Refund on Failure
            wallet.setBalance(wallet.getBalance().add(amountTrx));
            fundingWalletRepository.save(wallet);
            throw new RuntimeException("Transaction failed, refunded balance. Error: " + e.getMessage());
        }

        return txid;
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
