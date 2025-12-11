package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "futures_positions")
public class FuturesPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(nullable = false)
    private String symbol; // BTCUSDT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PositionSide side; // LONG, SHORT

    @Column(precision = 24, scale = 8)
    private BigDecimal entryPrice;

    @Column(precision = 24, scale = 8)
    private BigDecimal quantity;

    private int leverage;

    @Column(precision = 24, scale = 8)
    private BigDecimal margin; // Isolated margin

    @Column(precision = 24, scale = 8)
    private BigDecimal liquidationPrice;

    @Enumerated(EnumType.STRING)
    private PositionStatus status; // OPEN, CLOSED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PositionSide {
        LONG, SHORT
    }

    public enum PositionStatus {
        OPEN, CLOSED, LIQUIDATED
    }
}
