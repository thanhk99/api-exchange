package api.exchange.dtos.Response;

import java.math.BigDecimal;
import java.util.List;

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
    private BigDecimal totalTransfer;
    private BigDecimal percentComplete;
    private double percentLike;
    private BigDecimal price;
    private BigDecimal availableAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private List<api.exchange.models.PaymentMethod> paymentMethods;
    private String asset;
    private String fiatCurrency;
    private TradeType tradeType;

}
