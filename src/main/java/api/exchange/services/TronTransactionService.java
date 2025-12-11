package api.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.proto.Chain.Transaction;
import org.tron.trident.proto.Response.TransactionExtention;

@Service
public class TronTransactionService {

    @Autowired
    private TronWalletService walletService;

    // Send TRX from a System User (Custodial)
    public String sendTrx(String userId, String toAddress, long amount) {
        ApiWrapper wrapper = null;
        try {
            // Retrieve and decrypt private key internally
            String fromPrivateKey = walletService.getDecryptedPrivateKey(userId);

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
        String fromPrivateKey = walletService.getDecryptedPrivateKey(userId);

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
}
