package api.exchange.dtos.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KlinesFuturesResponse {
    private String symbol;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private long startTime;
    private long closeTime;
    private String interval;
    private boolean isClosed;
}
