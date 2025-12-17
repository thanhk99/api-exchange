package api.exchange.dtos.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TronWalletResponse {
    private String userId;
    private String address;
}
