package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallets_funding")
public class FundingWallet {
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

    // Fields merged from TronWallet
    @Column(name = "address")
    private String address;

    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;

    @Column(name = "hex_address")
    private String hexAddress;

    public BigDecimal getAvailableBalance() {
        return balance != null ? balance : BigDecimal.ZERO;
    }

    public BigDecimal getTotalBalance() {
        return (balance != null ? balance : BigDecimal.ZERO)
                .add(lockedBalance != null ? lockedBalance : BigDecimal.ZERO);
    }
}