package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "futures_funding_rates")
public class FuturesFundingRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String symbol; // BTCUSDT

    @Column(precision = 24, scale = 8)
    private BigDecimal rate; // e.g., 0.0001 (0.01%)

    private LocalDateTime timestamp; // Time of funding round

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
