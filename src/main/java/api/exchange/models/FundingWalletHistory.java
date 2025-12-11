package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "funding_wallet_historys")
public class FundingWalletHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "asset", nullable = false)
    private String asset;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "create_dt", nullable = false)
    private LocalDateTime createDt;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "status")
    private String status;

    @Column(name = "address")
    private String address;

    @Column(name = "fee")
    private BigDecimal fee;

    @Column(name = "note")
    private String note;
}
