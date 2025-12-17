package api.exchange.services;

import api.exchange.models.FundingWallet;
import api.exchange.repository.FundingWalletHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Service
public class TronDepositScheduler {

    @Autowired
    private FundingWalletHistoryRepository fundingWalletHistoryRepository;

    @Autowired
    private FundingWalletService fundingWalletService;

    @Autowired
    private TronTransactionService tronTransactionService;

    @Autowired
    private api.exchange.repository.FundingWalletRepository fundingWalletRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Run every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void checkDeposits() {
        List<FundingWallet> wallets = fundingWalletRepository.findAllByCurrency("TRX");

        for (FundingWallet wallet : wallets) {
            if (wallet.getAddress() != null) {
                checkWalletDeposits(wallet);
            }
        }
    }

    public List<JsonNode> fetchTransactions(FundingWallet wallet) {
        List<JsonNode> transactionList = new java.util.ArrayList<>();
        try {
            String url = "https://nile.trongrid.io/v1/accounts/" + wallet.getAddress()
                    + "/transactions?limit=50&visible=true"; // Removed only_to=true to get send history too

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.get("data");

                if (data != null && data.isArray()) {
                    for (JsonNode tx : data) {
                        transactionList.add(tx);
                    }
                }
            } else {
                log.error("Failed to fetch transactions for {}: {}", wallet.getAddress(), response.statusCode());
            }

        } catch (Exception e) {
            log.error("Error checking deposits for wallet {}", wallet.getAddress(), e);
        }
        return transactionList;
    }

    private void checkWalletDeposits(FundingWallet wallet) {
        List<JsonNode> transactions = fetchTransactions(wallet);
        for (JsonNode tx : transactions) {
            processTransaction(tx, wallet);
        }
    }

    private void processTransaction(JsonNode tx, FundingWallet wallet) {
        try {
            String txId = tx.get("txID").asText();

            // Check if already processed using FundingWalletHistory note field
            api.exchange.models.FundingWalletHistory existing = fundingWalletHistoryRepository
                    .findFirstByNoteContaining(txId);

            if (existing != null) {
                // If it exists and is a Withdrawal ("Rút tiền") with 0 Fee, try to update the
                // Fee
                if ("Rút tiền".equals(existing.getType())
                        && (existing.getFee() == null || existing.getFee().compareTo(BigDecimal.ZERO) == 0)) {
                    updateTransactionFee(existing, txId);
                }
                return;
            }

            // Check status
            if (tx.has("ret")) {
                JsonNode ret = tx.get("ret").get(0);
                String status = ret.get("contractRet").asText();
                if (!"SUCCESS".equals(status)) {
                    return;
                }
            }

            // Check raw data
            JsonNode rawData = tx.get("raw_data");
            long timestamp = rawData.get("timestamp").asLong();
            JsonNode contract = rawData.get("contract").get(0);
            String type = contract.get("type").asText();
            JsonNode value = contract.get("parameter").get("value");

            if ("TransferContract".equals(type)) {
                // TRX Transfer
                String toAddress = value.get("to_address").asText();

                // Compare with both Base58 (address) and Hex (hexAddress) to be safe
                boolean isMatch = wallet.getAddress().equals(toAddress);
                if (!isMatch && wallet.getHexAddress() != null) {
                    isMatch = wallet.getHexAddress().equals(toAddress);
                }

                if (isMatch) {
                    long amountSun = value.get("amount").asLong();
                    BigDecimal amountTrx = BigDecimal.valueOf(amountSun).divide(BigDecimal.valueOf(1_000_000));
                    String ownerAddress = value.get("owner_address").asText(); // Usually Hex in raw_data

                    // Credit user
                    log.info("Processing TRX deposit: {} TRX for user {}", amountTrx, wallet.getUid());
                    creditUser(wallet.getUid(), amountTrx, "TRX", txId, ownerAddress, timestamp, "SUCCESS",
                            BigDecimal.ZERO);
                }
            }
            // Add TRC20 logic here later (TriggerSmartContract) if needed
            // Add TRC20 logic here later (TriggerSmartContract) if needed

        } catch (Exception e) {
            log.error("Error processing transaction", e);
        }
    }

    private void updateTransactionFee(api.exchange.models.FundingWalletHistory history, String txId) {
        try {
            long feeSun = tronTransactionService.getTransactionFee(txId);
            if (feeSun > 0) {
                BigDecimal feeTrx = BigDecimal.valueOf(feeSun).divide(BigDecimal.valueOf(1_000_000));
                history.setFee(feeTrx);
                // Also ensure status is SUCCESS since we found it on chain
                history.setStatus("SUCCESS");
                fundingWalletService.updateHistory(history);
                log.info("Updated fee for tx {}: {} TRX", txId, feeTrx);
            }
        } catch (Exception e) {
            log.error("Failed to update fee for tx {}", txId, e);
        }
    }

    private void creditUser(String uid, BigDecimal amount, String currency, String txId, String fromAddress,
            long timestamp, String status, BigDecimal fee) {
        // Update Funding Wallet with Note containing TxID
        FundingWallet fundingWallet = new FundingWallet();
        fundingWallet.setUid(uid);
        fundingWallet.setCurrency(currency);
        fundingWallet.setBalance(amount);

        String timeStr = java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String note = String.format("Deposit %s %s from %s | Time: %s | TxID: %s",
                amount, currency, fromAddress, timeStr, txId);

        fundingWalletService.addBalanceCoin(fundingWallet, note, status, fromAddress, fee, txId); // Pass txId as hash
    }
}
