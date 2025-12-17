package api.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response.TransactionExtention;

import api.exchange.models.FundingWallet;
import api.exchange.repository.FundingWalletRepository;
import jakarta.transaction.Transactional;

@Service
public class TronTransactionService {

    @Autowired
    private FundingWalletRepository fundingWalletRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Send TRX from a System User (Custodial)
    public String sendTrx(String userId, String toAddress, long amount) {
        ApiWrapper wrapper = null;
        try {
            // Retrieve and decrypt private key internally
            FundingWallet wallet = fundingWalletRepository.findByUidAndCurrency(userId, "TRX");
            if (wallet == null || wallet.getEncryptedPrivateKey() == null) {
                throw new RuntimeException("Wallet not found or no private key for user: " + userId);
            }
            String fromPrivateKey = encryptionService.decrypt(wallet.getEncryptedPrivateKey());

            // Use ApiWrapper.ofNile as requested
            wrapper = ApiWrapper.ofNile(fromPrivateKey);
            org.tron.trident.core.key.KeyPair keyPair = new org.tron.trident.core.key.KeyPair(fromPrivateKey);
            String fromAddress = keyPair.toBase58CheckAddress();

            // 1. Build
            // Convert amount from TRX to Sun (1 TRX = 1,000,000 Sun)
            long amountInSun = amount * 1_000_000L;
            TransactionExtention txnExt = wrapper.transfer(fromAddress, toAddress, amountInSun);

            // 2. Sign
            Transaction signedTxn = wrapper.signTransaction(txnExt);

            // 3. Broadcast
            String txid = wrapper.broadcastTransaction(signedTxn);

            return txid;

        } catch (Exception e) {
            throw new RuntimeException("Failed to send TRX", e);
        } finally {
            if (wrapper != null) {
                wrapper.close();
            }
        }
    }

    public String sendTrc20(String userId, String toAddress, String contractAddress, long amount) {
        // Retrieve and decrypt private key internally
        FundingWallet wallet = fundingWalletRepository.findByUidAndCurrency(userId, "TRX");
        if (wallet == null || wallet.getEncryptedPrivateKey() == null) {
            throw new RuntimeException("Wallet not found or no private key for user: " + userId);
        }
        // String fromPrivateKey =
        // encryptionService.decrypt(wallet.getEncryptedPrivateKey());

        throw new RuntimeException("TRC20 transfer not yet implemented for this library version.");
    }

    public org.tron.trident.proto.Chain.Transaction getTransactionById(String txid) {
        ApiWrapper wrapper = null;
        try {
            // Read-only wrapper
            // Using the key found in original code, simplified to use ofNile for
            // consistency if possible,
            // but sticking to original constructor to avoid changing behavior too much,
            // just adding close().
            wrapper = new ApiWrapper(api.exchange.config.TronConfig.TRON_FULL_NODE,
                    api.exchange.config.TronConfig.TRON_SOLIDITY_NODE,
                    "5c42289c894957e849405d429a888065096a6668740c4a0378b8748383a15286");
            return wrapper.getTransactionById(txid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transaction info", e);
        } finally {
            if (wrapper != null)
                wrapper.close();
        }
    }

    public long getTransactionFee(String txid) {
        ApiWrapper wrapper = null;
        try {
            // Using same key
            wrapper = ApiWrapper.ofNile("5c42289c894957e849405d429a888065096a6668740c4a0378b8748383a15286");
            org.tron.trident.proto.Response.TransactionInfo info = wrapper.getTransactionInfoById(txid);
            return info.getFee();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transaction fee for " + txid, e);
        } finally {
            if (wrapper != null)
                wrapper.close();
        }
    }

    public java.math.BigDecimal estimateTrxFee(String userId, String toAddress, long amount) {
        ApiWrapper wrapper = null;
        try {
            // Retrieve and decrypt private key internally
            FundingWallet wallet = fundingWalletRepository.findByUidAndCurrency(userId, "TRX");
            if (wallet == null || wallet.getEncryptedPrivateKey() == null) {
                throw new RuntimeException("Wallet not found or no private key for user: " + userId);
            }
            String fromPrivateKey = encryptionService.decrypt(wallet.getEncryptedPrivateKey());

            wrapper = ApiWrapper.ofNile(fromPrivateKey);
            org.tron.trident.core.key.KeyPair keyPair = new org.tron.trident.core.key.KeyPair(fromPrivateKey);
            String fromAddress = keyPair.toBase58CheckAddress();

            // 1. Build Transaction to get size
            long amountInSun = amount * 1_000_000L;
            TransactionExtention txnExt = wrapper.transfer(fromAddress, toAddress, amountInSun);
            Transaction txn = txnExt.getTransaction();

            // Calculate Bandwidth needed
            // Serialized size + ~68 bytes for signature
            long bandwidthNeeded = txn.getSerializedSize() + 68;

            // 2. Check Account Resources (Bandwidth)
            org.tron.trident.proto.Response.AccountResourceMessage resources = wrapper.getAccountResource(fromAddress);

            long freeNetLimit = resources.getFreeNetLimit();
            long freeNetUsed = resources.getFreeNetUsed();
            long freeNetAvailable = freeNetLimit - freeNetUsed;

            long netLimit = resources.getNetLimit();
            long netUsed = resources.getNetUsed();
            long netAvailable = netLimit - netUsed;

            // If we have enough Free Bandwidth OR Trx-Frozen Bandwidth
            if (freeNetAvailable >= bandwidthNeeded || netAvailable >= bandwidthNeeded) {
                return java.math.BigDecimal.ZERO;
            }

            // Otherwise, pay with TRX. Current fee is 1000 SUN per byte.
            // 1000 SUN = 0.001 TRX
            long feeInSun = bandwidthNeeded * 1_000L;

            return java.math.BigDecimal.valueOf(feeInSun).divide(java.math.BigDecimal.valueOf(1_000_000L));

        } catch (Exception e) {
            // Fallback safety: return standard fee (~0.3 TRX) if check fails, or throw
            // Ideally assume worst case
            return java.math.BigDecimal.valueOf(0.3); // Safe fallback
        } finally {
            if (wrapper != null) {
                wrapper.close();
            }
        }
    }

    @Autowired
    private api.exchange.repository.FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Transactional
    public String transfer(String userId, String toAddress, long amount) {
        // 1. Validate and Retrieve Wallet
        java.math.BigDecimal amountTrx = java.math.BigDecimal.valueOf(amount);
        FundingWallet wallet = fundingWalletRepository.findByUidAndCurrency(userId, "TRX");

        if (wallet == null) {
            throw new RuntimeException("Wallet not found for user: " + userId);
        }

        // 2. Estimate Fee
        java.math.BigDecimal fee = estimateTrxFee(userId, toAddress, amount);

        // 3. Validate Balance (Amount + Fee)
        java.math.BigDecimal totalDeduction = amountTrx.add(fee);

        if (wallet.getBalance().compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance. Need " + totalDeduction + " TRX (incl. " + fee + " fee)");
        }

        // 4. Deduct Balance
        wallet.setBalance(wallet.getBalance().subtract(totalDeduction));
        fundingWalletRepository.save(wallet);

        String txid;
        try {
            // 5. Send Transaction
            txid = sendTrx(userId, toAddress, amount);

            // 6. Record Successful History
            api.exchange.models.FundingWalletHistory history = new api.exchange.models.FundingWalletHistory();
            history.setUserId(userId);
            history.setAsset("TRX");
            history.setAmount(amountTrx.negate());
            history.setBalance(wallet.getBalance());
            history.setType("Rút tiền");
            history.setCreateDt(java.time.LocalDateTime.now());
            history.setNote("Withdraw to " + toAddress + " | TxID: " + txid + " | Fee: " + fee);
            history.setStatus("SUCCESS");
            history.setAddress(toAddress);
            history.setFee(fee);
            history.setHash(txid);
            fundingWalletHistoryRepository.save(history);

        } catch (Exception e) {
            // 7. Refund on Failure (Amount + Fee)
            wallet.setBalance(wallet.getBalance().add(totalDeduction));
            fundingWalletRepository.save(wallet);
            throw new RuntimeException("Transaction failed, refunded balance. Error: " + e.getMessage());
        }

        return txid;
    }
}
