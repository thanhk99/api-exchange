package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_earn_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarnOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id")
    // private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private EarnProduct product;

    @Column(precision = 24, scale = 8)
    private BigDecimal amount;

    private Integer durationDays;
}