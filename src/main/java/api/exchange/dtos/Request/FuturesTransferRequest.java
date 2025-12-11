package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FuturesTransferRequest {
    private BigDecimal amount;
    private String type; // "TO_FUTURES" or "FROM_FUTURES"
}
