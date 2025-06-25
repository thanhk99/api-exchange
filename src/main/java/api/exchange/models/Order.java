package api.exchange.models;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id")
    private P2PAd ad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "uid", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "uid", nullable = false)
    private User seller;

    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "fiat_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal fiatAmount;

    @Column(precision = 20, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_payer")
    private FeePayer feePayer = FeePayer.BUYER;

    @Column(name = "escrow_address", length = 255)
    private String escrowAddress;

    @Column(name = "payment_proof", columnDefinition = "text")
    private String paymentProof;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "chat_id", length = 36)
    private String chatId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum OrderStatus {
        PENDING, PAID, CANCELLED, COMPLETED, DISPUTE, REFUNDED
    }

    public enum FeePayer {
        BUYER, SELLER, SPLIT
    }
}