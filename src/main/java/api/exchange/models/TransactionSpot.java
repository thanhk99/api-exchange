package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "spot_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

public class TransactionSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quoteQuantity;

    @Column(nullable = false)
    private Long buyerOrderId;

    @Column(nullable = false)
    private Long sellerOrderId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(precision = 18, scale = 8)
    private BigDecimal fee;

    @Column(length = 10)
    private String feeAsset;

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    private TradeSide side;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    private Boolean isMaker;

    public enum TradeSide {
        BUY, SELL
    }

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
        if (quoteQuantity == null) {
            quoteQuantity = price.multiply(quantity);
        }
    }
}
