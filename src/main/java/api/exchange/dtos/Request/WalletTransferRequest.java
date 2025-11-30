package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletTransferRequest {
    private String fromWallet; // "FUNDING" or "SPOT"
    private String toWallet; // "FUNDING" or "SPOT"
    private String currency;
    private BigDecimal amount;
}
