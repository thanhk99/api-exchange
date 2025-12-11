package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "futures_orders")
public class FuturesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    private OrderSide side; // BUY, SELL

    @Enumerated(EnumType.STRING)
    private PositionSide positionSide; // LONG, SHORT

    @Enumerated(EnumType.STRING)
    private OrderType type; // MARKET, LIMIT

    @Column(precision = 24, scale = 8)
    private BigDecimal price;

    @Column(precision = 24, scale = 8)
    private BigDecimal quantity;

    private int leverage;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

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

    public enum OrderSide {
        BUY, SELL
    }

    public enum PositionSide {
        LONG, SHORT
    }

    public enum OrderType {
        MARKET, LIMIT, STOP_MARKET, STOP_LIMIT
    }

    public enum OrderStatus {
        PENDING, FILLED, CANCELLED, PARTIALLY_FILLED
    }
}
