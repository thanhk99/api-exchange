package api.exchange.dtos.Request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotKlineRequest {

    private String symbol;
    private String interval;
}
