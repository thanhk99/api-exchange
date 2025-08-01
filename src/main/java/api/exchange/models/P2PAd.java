package api.exchange.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "p2p_ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class P2PAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JoinColumn(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String asset; // USDT, BTC...

    @Column(name = "fiat_currency", nullable = false, columnDefinition = "VARCHAR(10)")
    private String fiatCurrency; // VND, USD...

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, columnDefinition = "VARCHAR(10)")
    private TradeType tradeType;

    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type")
    private PriceType priceType = PriceType.FIXED;

    @Column(name = "min_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal maxAmount;

    @Column(name = "available_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal availableAmount;

    @Column(name = "payment_methods", nullable = false)
    private String paymentMethods; // JSON: ["BankTransfer", "Momo"]

    @Column(name = "terms_conditions", columnDefinition = "text")
    private String termsConditions;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // @OneToMany(mappedBy = "ad", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Order> orders;

    public enum TradeType {
        BUY, SELL
    }

    public enum PriceType {
        FIXED, DYNAMIC
    }
}