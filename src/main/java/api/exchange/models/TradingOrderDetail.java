package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_trading_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    // private Order order;

    @Column(length = 20)
    private String pair;

    @Column(precision = 24, scale = 8)
    private BigDecimal price;

    @Column(precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(precision = 24, scale = 8)
    private BigDecimal filledAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private TradingOrderType orderType;

    public enum TradingOrderType {
        LIMIT, MARKET, STOP_LOSS
    }
}