package api.exchange.dtos.Request;

import api.exchange.models.FuturesOrder;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FuturesOrderRequest {
    private String symbol;
    private FuturesOrder.OrderSide side;
    private FuturesOrder.PositionSide positionSide;
    private FuturesOrder.OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private int leverage;
}
