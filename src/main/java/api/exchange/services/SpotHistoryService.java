package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.SpotHistory;
import api.exchange.models.SpotHistory.TradeType;
import api.exchange.repository.SpotHistoryRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SpotHistoryService {

    @Autowired
    private SpotHistoryRepository SpotHistoryRepository;

    @Transactional
    public SpotHistory createSpotRecord(String symbol, BigDecimal price, BigDecimal quantity,
            Long buyOrderId, Long sellOrderId, TradeType TradeType) {

        BigDecimal total = price.multiply(quantity);

        SpotHistory Spot = SpotHistory.builder()
                .symbol(symbol)
                .price(price)
                .quantity(quantity)
                .total(total)
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .tradeType(TradeType)
                .createdAt(LocalDateTime.now())
                .build();

        SpotHistory savedSpot = SpotHistoryRepository.save(Spot);
        log.info("📝 Saved Spot history: {}", savedSpot.getId());

        return savedSpot;
    }

    public List<SpotHistory> getSpotHistoryBySymbol(String symbol) {
        return SpotHistoryRepository.findBySymbolOrderByCreatedAtDesc(symbol);
    }

    public List<SpotHistory> getSpotHistoryByOrderId(Long orderId) {
        return SpotHistoryRepository.findByBuyOrderIdOrSellOrderId(orderId, orderId);
    }

    public List<SpotHistory> getSpotHistoryByPeriod(String symbol, LocalDateTime start, LocalDateTime end) {
        return SpotHistoryRepository.findBySymbolAndCreatedAtBetween(symbol, start, end);
    }

    public BigDecimal getLastSpotdPrice(String symbol) {
        List<SpotHistory> recentSpots = SpotHistoryRepository.findBySymbolOrderByCreatedAtDesc(symbol);
        if (!recentSpots.isEmpty()) {
            return recentSpots.get(0).getPrice();
        }
        return null;
    }

    public BigDecimal get24hVolume(String symbol) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<SpotHistory> Spots = SpotHistoryRepository.findBySymbolAndCreatedAtBetween(symbol, twentyFourHoursAgo,
                LocalDateTime.now());

        return Spots.stream()
                .map(SpotHistory::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}