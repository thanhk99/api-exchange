package api.exchange.services;

import api.exchange.models.FuturesPosition;
import api.exchange.repository.FuturesPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LiquidationService {

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private CoinDataService coinDataService;

    @Scheduled(fixedRate = 1000) // Run every second
    @Transactional
    public void checkLiquidations() {
        // In a real system, we would iterate efficiently or use a queue.
        // For MVP, we fetch all open positions.
        List<FuturesPosition> openPositions = futuresPositionRepository.findAll(); // Should filter by status=OPEN

        for (FuturesPosition position : openPositions) {
            if (position.getStatus() != FuturesPosition.PositionStatus.OPEN)
                continue;

            BigDecimal currentPrice = coinDataService.getCurrentPrice(position.getSymbol());
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0)
                continue;

            boolean shouldLiquidate = false;
            if (position.getSide() == FuturesPosition.PositionSide.LONG) {
                if (currentPrice.compareTo(position.getLiquidationPrice()) <= 0) {
                    shouldLiquidate = true;
                }
            } else {
                if (currentPrice.compareTo(position.getLiquidationPrice()) >= 0) {
                    shouldLiquidate = true;
                }
            }

            if (shouldLiquidate) {
                liquidatePosition(position, currentPrice);
            }
        }
    }

    private void liquidatePosition(FuturesPosition position, BigDecimal currentPrice) {
        position.setStatus(FuturesPosition.PositionStatus.LIQUIDATED);
        // In real system, we might sell into the market or insurance fund takes over.
        // Here we just close it.
        // Margin is lost (already locked in wallet, we don't return it).

        futuresPositionRepository.save(position);
        System.out.println("💥 LIQUIDATED Position: " + position.getId() + " Symbol: " + position.getSymbol()
                + " Price: " + currentPrice);
    }
}
