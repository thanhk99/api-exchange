package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SwapRequest {
    private String fromCoin;
    private String toCoin;
    private BigDecimal amount;
}
