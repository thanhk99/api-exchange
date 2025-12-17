package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "futures_transactions")
public class FuturesTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency; // USDT

    private String referenceId; // OrderID or PositionID

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        TRANSFER_IN, TRANSFER_OUT, REALIZED_PNL, FUNDING_FEE, COMMISSION, LIQUIDATION_CLEARANCE
    }
}
