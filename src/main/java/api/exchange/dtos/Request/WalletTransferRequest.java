package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletTransferRequest {
    private String fromWallet; // "FUNDING", "SPOT", "FUTURES"
    private String toWallet; // "FUNDING", "SPOT", "FUTURES"
    private String currency;
    private BigDecimal amount;
}
