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
        try {
            // Retrieve and decrypt private key internally
            String fromPrivateKey = walletService.getDecryptedPrivateKey(userId);

            // Use config constants to connect to the selected network (Nile)
            ApiWrapper wrapper = new ApiWrapper(api.exchange.config.TronConfig.TRON_FULL_NODE,
                    api.exchange.config.TronConfig.TRON_SOLIDITY_NODE,
                    fromPrivateKey);
            KeyPair keyPair = new KeyPair(fromPrivateKey);
            String fromAddress = keyPair.toBase58CheckAddress();

            // 1. Build
            TransactionExtention txnExt = wrapper.transfer(fromAddress, toAddress, amount);

            // 2. Sign
            Transaction signedTxn = wrapper.signTransaction(txnExt);

            // 3. Broadcast
            String txid = wrapper.broadcastTransaction(signedTxn);

            return txid;

        } catch (Exception e) {
            throw new RuntimeException("Failed to send TRX", e);
        }
    }

    public String sendTrc20(String userId, String toAddress, String contractAddress, long amount) {
        // Retrieve and decrypt private key internally
        String fromPrivateKey = walletService.getDecryptedPrivateKey(userId);

        throw new RuntimeException("TRC20 transfer not yet implemented for this library version.");
    }

    public org.tron.trident.proto.Chain.Transaction getTransactionById(String txid) {
        try {
            // Read-only wrapper
            ApiWrapper wrapper = new ApiWrapper(api.exchange.config.TronConfig.TRON_FULL_NODE,
                    api.exchange.config.TronConfig.TRON_SOLIDITY_NODE,
                    "5c42289c894957e849405d429a888065096a6668740c4a0378b8748383a15286");
            return wrapper.getTransactionById(txid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transaction info", e);
        }
    }
}
