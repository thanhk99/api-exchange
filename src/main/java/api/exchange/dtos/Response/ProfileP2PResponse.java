package api.exchange.dtos.Response;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileP2PResponse {
   
    private BigDecimal coinAvaiable;
    private BigDecimal coinBlock;
    private BigDecimal totalOrderDone30Days ;
    private BigDecimal buyOrderDone30Days ;
    private BigDecimal sellOrderDone30Days ; 
    private BigDecimal percnetTotalDone30Days;
    private BigDecimal percnetBuyDone30Days;
    private BigDecimal percnetSellDone30Days;
    private BigDecimal totalOrderDone ;
    private BigDecimal totalOrderDoneValue ;
    private BigDecimal percnetTotalDone;
    private BigDecimal averagePayTime;
    private BigDecimal positiveReviewRate ; 
    private BigDecimal positiveReview;
    private BigDecimal negativeReview;
    private BigDecimal flower ; 
    private BigDecimal  blocker;
}
