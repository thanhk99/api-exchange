package api.exchange.dtos.Response;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KlinesSpotResponse {
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
