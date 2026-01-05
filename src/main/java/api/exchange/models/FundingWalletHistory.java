package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "funding_wallet_historys", indexes = {
        @Index(name = "idx_funding_ledger_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_funding_ledger_user_asset", columnList = "user_id, asset")
})
public class FundingWalletHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "asset", nullable = false)
    private String asset;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false, precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(name = "balance", nullable = false, precision = 24, scale = 8)
    private BigDecimal balance;

    @Column(name = "create_dt")
    private LocalDateTime createDt;

    @Column(name = "note")
    private String note;

    @Column(name = "status")
    private String status;

    @Column(name = "address")
    private String address;

    @Column(name = "fee", precision = 24, scale = 8)
    private BigDecimal fee;

    @Column(name = "hash")
    private String hash;
}
