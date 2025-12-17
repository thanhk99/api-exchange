package api.exchange.dtos.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FuturesMarketResponse {
    private String symbol;
    private BigDecimal markPrice;
    private BigDecimal indexPrice;
    private BigDecimal lastPrice;
    private BigDecimal priceChange24h;
    private BigDecimal priceChangePercent;
    private BigDecimal highPrice24h;
    private BigDecimal lowPrice24h;
    private BigDecimal volume24h; // Base Volume (e.g. BTC)
    private BigDecimal quoteVolume24h; // Quote Volume (e.g. USDT)
    private BigDecimal openInterest; // Open Interest in USDT
    private BigDecimal marketCap; // Market Cap in USDT
    private BigDecimal fundingRate;
    private LocalDateTime nextFundingTime;
    private String logoUrl;
    private long timestamp;
}
