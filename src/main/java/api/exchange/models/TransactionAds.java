package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions_ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAds {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "buyer_id")
    private String buyerId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false )
    private String asset;

    @Column(name = "coin_amount")
    private BigDecimal coinAmount;

    @Column(name = "fiat_amount")
    private BigDecimal fiatAmount;

    @Column(precision = 24, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancle_by")
    private String cancleBy;

    @Column(name = "ads_id", nullable = false)
    private long adsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private status status;

    private LocalDateTime createdAt;

    @Column(name = "complete_at")
    private LocalDateTime completeAt;

    public enum status {
        PENDING, CANCLELED, DONE;
    }

    public enum cancleBy{
        SELLER , BUYER , SYSTEM , ADMIN
    }

}
