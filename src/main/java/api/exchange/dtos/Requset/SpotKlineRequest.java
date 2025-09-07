package api.exchange.dtos.Requset;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotKlineRequest {

    private String symbol;
    private String interval;
}
