package api.exchange.dtos.Response;

import java.math.BigDecimal;

import api.exchange.models.P2PAd.TradeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserP2PResponse {
    private Long id;
    private String uid;
    private String name;
    private Long totalTransfer;
    private double percentComplete;
    private double percentLike;
    private BigDecimal price;
    private BigDecimal availableAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String paymentMethod;
    private String asset;
    private String fiatCurrency;
    private TradeType tradeType;

}
