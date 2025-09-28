package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBooks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 36, scale = 18)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "order_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String uid;

    public enum OrderType {
        BUY, SELL
    }

    public enum OrderStatus {
        FILLED, PARTIALLY_FILLED, ACTIVE, CANCELLED, DONE
    }

    public enum TradeType {
        LIMIT, MARKET
    }

    public boolean isBuyOrder() {
        return orderType == OrderType.BUY;
    }

    public boolean isSellOrder() {
        return orderType == OrderType.SELL;
    }

    public boolean isLimitOrder() {
        return tradeType == TradeType.LIMIT;
    }

    public boolean isMarketOrder() {
        return tradeType == TradeType.MARKET;
    }

    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    public boolean isFullyFilled() {
        return getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0;
    }
}
