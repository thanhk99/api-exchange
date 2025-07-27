package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets_earn")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarnWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Column(name = "uid", nullable = false)
    private Long uid;

    // @ManyToOne
    // @JoinColumn(name = "product_id")
    // private EarnProduct product;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_balance", precision = 24, scale = 8)
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(name = "avaiable_balance")
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance ")
    private BigDecimal lockedBalance = BigDecimal.ZERO;
}