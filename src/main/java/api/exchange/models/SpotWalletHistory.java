package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "spot_wallet_history")
public class SpotWalletHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "asset", nullable = false)
    private String asset;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "create_dt", nullable = false)
    private LocalDateTime createDt;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "note")
    private String note;
}
