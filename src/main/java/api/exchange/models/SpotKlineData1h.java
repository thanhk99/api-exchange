package api.exchange.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spot_kline_data_1h")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotKlineData1h {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "open_price", precision = 20, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "close_price", precision = 20, scale = 8)
    private BigDecimal closePrice;

    @Column(name = "high_price", precision = 20, scale = 8)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 20, scale = 8)
    private BigDecimal lowPrice;

    @Column(name = "volume", precision = 20, scale = 8)
    private BigDecimal volume;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Column(name = "is_closed")
    private Boolean isClosed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

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
