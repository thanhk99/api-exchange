package api.exchange.models;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_trading_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingOrderDetail {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @Column(length = 20)
    private String pair; // e.g., "BTC/USDT"

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