package api.exchange.models;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets_trading")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingWallet {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID walletId;

    @ManyToOne
    @JoinColumn(name = "uid", referencedColumnName = "uid", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(precision = 24, scale = 8)
    private BigDecimal available = BigDecimal.ZERO;

    @Column(precision = 24, scale = 8)
    private BigDecimal locked = BigDecimal.ZERO;

    private boolean marginEnabled = false;
}