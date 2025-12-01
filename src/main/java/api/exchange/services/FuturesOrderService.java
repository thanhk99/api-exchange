package api.exchange.services;

import api.exchange.models.FuturesOrder;
import api.exchange.repository.FuturesOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FuturesOrderService {

    @Autowired
    private FuturesOrderRepository futuresOrderRepository;

    @Autowired
    private FuturesTradingService futuresTradingService;

    @Autowired
    private CoinDataService coinDataService;

    @Scheduled(fixedRate = 1000) // Run every second
    @Transactional
    public void matchLimitOrders() {
        // In a real system, we would use an Order Book or more efficient querying.
        // For MVP, we fetch all pending orders.
        List<FuturesOrder> pendingOrders = futuresOrderRepository.findAll(); // Should filter by status=PENDING

        for (FuturesOrder order : pendingOrders) {
            if (order.getStatus() != FuturesOrder.OrderStatus.PENDING)
                continue;
            if (order.getType() == FuturesOrder.OrderType.MARKET)
                continue; // Should not happen if filtered correctly

            BigDecimal currentPrice = coinDataService.getCurrentPrice(order.getSymbol());
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0)
                continue;

            boolean shouldExecute = false;

            // Simple Limit Matching Logic
            if (order.getSide() == FuturesOrder.OrderSide.BUY) {
                // Buy Limit: Execute if Current Price <= Limit Price
                if (currentPrice.compareTo(order.getPrice()) <= 0) {
                    shouldExecute = true;
                }
            } else {
                // Sell Limit: Execute if Current Price >= Limit Price
                if (currentPrice.compareTo(order.getPrice()) >= 0) {
                    shouldExecute = true;
                }
            }

            if (shouldExecute) {
                System.out.println("⚡ MATCHED Limit Order: " + order.getId() + " Symbol: " + order.getSymbol()
                        + " Price: " + currentPrice);
                futuresTradingService.executeOrder(order, currentPrice);
            }
        }
    }
}
