package api.exchange.dtos.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuturesKlineRequest {
    private String symbol;
    private String interval;
}
