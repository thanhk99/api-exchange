package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InternalTransferRequest {
    private String recipientIdentifier; // UID, Email, or Phone
    private String currency;
    private BigDecimal amount;
}
