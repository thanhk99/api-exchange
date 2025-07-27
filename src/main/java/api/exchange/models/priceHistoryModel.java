package api.exchange.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "price_history", uniqueConstraints = {
                @UniqueConstraint(name = "uc_symbol_timestamp", columnNames = { "symbol", "timestamp" })
}, indexes = {
                @Index(name = "idx_symbol_time", columnList = "symbol,timestamp DESC"),

                @Index(name = "idx_timestamp", columnList = "timestamp DESC"),

                @Index(name = "idx_interval_type", columnList = "interval_type")
})
public class priceHistoryModel implements Serializable {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "symbol")
        private String symbol;

        @Column(name = "open_price", precision = 20, scale = 6, nullable = false)
        private BigDecimal openPrice;

        @Column(name = "high_price", precision = 20, scale = 6, nullable = false)
        private BigDecimal highPrice;

        @Column(name = "low_price", precision = 20, scale = 6, nullable = false)
        private BigDecimal lowPrice;

        @Column(name = "close_price", precision = 20, scale = 6, nullable = false)
        private BigDecimal closePrice;

        @Column(precision = 20, scale = 2, nullable = false)
        private BigDecimal volume;

        @Column(name = "interval_type", nullable = false, length = 5)
        private String intervalType; // "1m", "5m", "1h", "1d"

        @Column(nullable = false)
        private LocalDateTime timestamp;
}