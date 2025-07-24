package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallets_spot")
public class SpotWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(precision = 24, scale = 8)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 24, scale = 8)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    private boolean isActive = true;
}