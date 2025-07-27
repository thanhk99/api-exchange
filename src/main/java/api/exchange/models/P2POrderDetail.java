package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_p2p_details")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class P2POrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id")
    // private Order order;

    @Column(length = 10)
    private String fiatCurrency; // "VND", "USD"

    @Column(precision = 24, scale = 8)
    private BigDecimal price;

    @Column(length = 50)
    private String paymentMethod; // "Bank Transfer", "Momo"

    private String advertisementId;
}
