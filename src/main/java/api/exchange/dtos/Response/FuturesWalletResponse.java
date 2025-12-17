package api.exchange.dtos.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuturesWalletResponse {
    private String currency;
    private BigDecimal balance; // Total balance
    private BigDecimal lockedBalance; // Margin used in positions
    private BigDecimal availableBalance; // balance - lockedBalance
    private BigDecimal unrealizedPnl; // Total unrealized profit/loss from open positions
    private BigDecimal totalPositionValue; // Total notional value of open positions
    private BigDecimal marginRatio; // (lockedBalance / balance) * 100
    private int openPositionsCount; // Number of open positions
}
