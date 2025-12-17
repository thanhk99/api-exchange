package api.exchange.dtos.Request;

import lombok.Data;

@Data
public class TronTransferRequest {
    private String type; // TRX or TRC20
    private String userId; // Who is sending
    private String toAddress;
    private String contractAddress; // Only for TRC20
    private long amount;
}
