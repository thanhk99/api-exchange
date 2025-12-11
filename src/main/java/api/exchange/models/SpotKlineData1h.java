package api.exchange.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spot_kline_data_1h")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SpotKlineData1h extends BaseKlineData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Constructor để tạo từ KlinesSpotResponse
    public SpotKlineData1h(String symbol, BigDecimal openPrice,
            BigDecimal closePrice, BigDecimal highPrice, BigDecimal lowPrice,
            BigDecimal volume, LocalDateTime startTime, LocalDateTime closeTime,
            Boolean isClosed) {
        this.symbol = symbol;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.startTime = startTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }
}
