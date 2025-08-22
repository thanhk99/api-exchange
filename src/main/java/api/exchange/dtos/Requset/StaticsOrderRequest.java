package api.exchange.dtos.Requset;

import java.time.LocalDate;
import api.exchange.models.TransactionAds.status;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StaticsOrderRequest {
    private String tradeType ;
    private LocalDate startDate;
    private LocalDate endDate ;
    private String status; 
}
