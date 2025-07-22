package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(name = "from_user")
    private String fromUser;

    @Column(name = "to_user")
    private String toUser;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "coin_amount")
    private BigDecimal coinAmount;

    @Column(name = "fiat_amount")
    private BigDecimal fiatAmount;

    @Column(precision = 24, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "cancle_by")
    private String cancleBy;

    @Column(name = "ads_id", nullable = false)
    private String adsId;

    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
