package api.exchange.dtos.Request;

import api.exchange.models.PaymentMethod.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {
    private PaymentType type;
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private String qrCode;
    private Boolean isDefault;
}
