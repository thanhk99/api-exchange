package api.exchange.models;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_prices", indexes = {
        @Index(name = "idx_crypto_currency", columnList = "crypto_id, currency"),
        @Index(name = "idx_last_updated", columnList = "last_updated")
})
@Data
public class CryptoPrice implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crypto_id")
    private String cryptoId;

    @Column(name = "currency")
    private String currency;

    @Column(name = "price", precision = 18, scale = 6)
    private BigDecimal price;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
