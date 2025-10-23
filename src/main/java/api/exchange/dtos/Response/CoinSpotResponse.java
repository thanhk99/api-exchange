package api.exchange.dtos.Response;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinSpotResponse {
    private String symbol;
    private BigDecimal price;
    private BigDecimal changePercent;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal volume;
    private Long timestamp;
}
