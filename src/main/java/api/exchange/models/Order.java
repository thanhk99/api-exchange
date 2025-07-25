package api.exchange.models;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_order_id", columnList = "order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false, referencedColumnName = "uid")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Composition relationships
    // @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private TradingOrderDetail tradingDetail;

    // @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private P2POrderDetail p2pDetail;

    // @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private EarnOrderDetail earnDetail;

    // Enums
    public enum OrderType {
        TRADING, P2P, EARN, OTC
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
}