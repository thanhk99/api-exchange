package api.exchange.services;

import api.exchange.models.TronWallet;
import api.exchange.repository.TronWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;

import java.util.Optional;

@Service
public class TronWalletService {

    @Autowired
    private ApiWrapper apiWrapper; // Used for balance check

    @Autowired
    private TronWalletRepository tronWalletRepository;

    @Autowired
    private EncryptionService encryptionService;

    public TronWallet createWallet(String uid) {
        // Check if user already has a wallet
        Optional<TronWallet> existingWallet = tronWalletRepository.findByUid(uid);
        if (existingWallet.isPresent()) {
            return existingWallet.get();
        }

        // Generate new key pair
        KeyPair keyPair = KeyPair.generate();
        String privateKey = keyPair.toPrivateKey();
        String publicKey = keyPair.toPublicKey(); // We might not store public key explicitly if address is derived
        String address = keyPair.toBase58CheckAddress();
        String hexAddress = keyPair.toHexAddress();

        // Encrypt private key
        String encryptedPrivateKey = encryptionService.encrypt(privateKey);

        // Save to DB
        TronWallet wallet = TronWallet.builder()
                .uid(uid)
                .address(address)
                .encryptedPrivateKey(encryptedPrivateKey)
                .hexAddress(hexAddress)
                .build();

        return tronWalletRepository.save(wallet);
    }

    public TronWallet getWallet(String uid) {
        return tronWalletRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + uid));
    }

    // Internal method to retrieve private key for transaction signing (used by
    // TransactionService)
    public String getDecryptedPrivateKey(String uid) {
        TronWallet wallet = getWallet(uid);
        return encryptionService.decrypt(wallet.getEncryptedPrivateKey());
    }

    public long getBalance(String address) {
        try {
            return apiWrapper.getAccountBalance(address);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching balance for address: " + address, e);
        }
    }

    public boolean validateAddress(String address) {
        try {
            return address != null && address.startsWith("T") && address.length() == 34;
        } catch (Exception e) {
            return false;
        }
    }
}
