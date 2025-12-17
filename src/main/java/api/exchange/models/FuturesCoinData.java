package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "futures_coin_data")
public class FuturesCoinData {
    @Id
    private String symbol; // BTCUSDT

    @Column(precision = 24, scale = 8)
    private BigDecimal markPrice; // Mark Price (giá thanh toán)

    @Column(precision = 24, scale = 8)
    private BigDecimal indexPrice; // Index Price (giá chỉ số)

    @Column(precision = 24, scale = 8)
    private BigDecimal lastPrice; // Last traded price

    @Column(precision = 24, scale = 8)
    private BigDecimal priceChange24h; // % thay đổi 24h

    @Column(precision = 24, scale = 8)
    private BigDecimal volume24h; // Khối lượng 24h

    @Column(precision = 24, scale = 8)
    private BigDecimal fundingRate; // Funding Rate hiện tại

    private LocalDateTime nextFundingTime; // Thời gian Funding tiếp theo

    private LocalDateTime lastUpdated;

    private String logoUrl; // Logo coin

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
