package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String fromWallet; // SPOT, FUNDING
    private String toWallet; // SPOT, FUNDING
    private String asset;
    private BigDecimal amount;
}
