package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallets_futures")
public class FuturesWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(nullable = false, length = 10)
    private String currency; // USDT

    @Column(precision = 24, scale = 8)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 24, scale = 8)
    private BigDecimal lockedBalance = BigDecimal.ZERO; // Margin used

    private boolean isActive = true;
}
