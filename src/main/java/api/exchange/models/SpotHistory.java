package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//not use
@Entity
@Table(name = "spot_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal total; // price * quantity

    @Column(name = "buy_order_id", nullable = false)
    private Long buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private Long sellOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType; // MARKET_MARKET, MARKET_LIMIT, LIMIT_LIMIT

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum TradeType {
        MARKET_MARKET,
        MARKET_LIMIT_BUY,
        MARKET_LIMIT_SELL,
        LIMIT_LIMIT
    }
}