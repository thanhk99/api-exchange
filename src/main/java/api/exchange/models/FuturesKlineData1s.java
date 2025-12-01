package api.exchange.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "futures_kline_data_1s")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FuturesKlineData1s extends BaseKlineData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Constructor custom with volume
    public FuturesKlineData1s(String symbol, BigDecimal price, BigDecimal volume, LocalDateTime time) {
        this.symbol = symbol;
        this.openPrice = price;
        this.closePrice = price;
        this.highPrice = price;
        this.lowPrice = price;
        this.volume = volume; // Real volume from aggregated trades
        this.startTime = time;
        this.closeTime = time;
        this.isClosed = true; // 1s data is considered closed immediately
    }
}
