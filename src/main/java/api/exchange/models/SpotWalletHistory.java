package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "spot_wallet_history", indexes = {
        @Index(name = "idx_spot_ledger_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_spot_ledger_user_asset", columnList = "user_id, asset")
})
public class SpotWalletHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // Liên kết với Trade ID, Order ID hoặc Transfer ID

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "asset", nullable = false)
    private String asset;

    @Column(name = "type", nullable = false)
    private String type; // Nạp tiền, Rút tiền, Trade, Transfer, ...

    @Column(name = "amount", nullable = false, precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(name = "balance", nullable = false, precision = 24, scale = 8)
    private BigDecimal balance; // Số dư sau giao dịch (Snapshot)

    @Column(name = "create_dt")
    private LocalDateTime createDt;

    @Column(name = "note")
    private String note;
}
