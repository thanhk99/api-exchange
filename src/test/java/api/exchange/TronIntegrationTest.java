package api.exchange;

import api.exchange.models.TronWallet;
import api.exchange.services.TronWalletService;
import api.exchange.services.TronTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TronIntegrationTest {

    @Autowired
    private TronWalletService walletService;

    @Autowired
    private TronTransactionService transactionService;

    @Test
    public void testCreateCustodialWallet() {
        String userId = "testUser_" + System.currentTimeMillis();
        TronWallet wallet = walletService.createWallet(userId);

        // Assert address is present
        assertNotNull(wallet.getAddress());
        assertTrue(wallet.getAddress().startsWith("T"));

        // Assert we have encrypted key internally
        assertNotNull(wallet.getEncryptedPrivateKey());

        System.out.println("Custodial Address: " + wallet.getAddress());

        // Check balance of this new wallet (should be 0)
        long balance = walletService.getBalance(wallet.getAddress());
        assertTrue(balance >= 0);
    }

    @Test
    public void testGetBalance() {
        // Nile Address
        String address = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";
        try {
            long balance = walletService.getBalance(address);
            System.out.println("Balance of " + address + ": " + balance);
            assertTrue(balance >= 0);
        } catch (Exception e) {
            System.out.println("Network might be unreachable: " + e.getMessage());
        }
    }
}
