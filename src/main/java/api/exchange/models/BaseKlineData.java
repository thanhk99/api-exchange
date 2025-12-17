package api.exchange.models;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseKlineData {

    @Column(name = "symbol", nullable = false, length = 20)
    protected String symbol;

    @Column(name = "open_price", precision = 20, scale = 8)
    protected BigDecimal openPrice;

    @Column(name = "close_price", precision = 20, scale = 8)
    protected BigDecimal closePrice;

    @Column(name = "high_price", precision = 20, scale = 8)
    protected BigDecimal highPrice;

    @Column(name = "low_price", precision = 20, scale = 8)
    protected BigDecimal lowPrice;

    @Column(name = "volume", precision = 20, scale = 8)
    protected BigDecimal volume;

    @Column(name = "start_time")
    protected LocalDateTime startTime;

    @Column(name = "close_time")
    protected LocalDateTime closeTime;

    @Column(name = "is_closed")
    protected Boolean isClosed;

    @Column(name = "created_at")
    protected LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
