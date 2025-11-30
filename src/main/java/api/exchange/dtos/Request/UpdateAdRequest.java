package api.exchange.dtos.Request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateAdRequest {
    private BigDecimal price;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal availableAmount;
    private List<String> paymentMethods;
    private Long paymentMethodId;
    private String termsConditions;
    private Boolean isActive;
}
