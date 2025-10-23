package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.SpotHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpotHistoryRepository extends JpaRepository<SpotHistory, Long> {

    List<SpotHistory> findBySymbolOrderByCreatedAtDesc(String symbol);

    List<SpotHistory> findByBuyOrderIdOrSellOrderId(Long buyOrderId, Long sellOrderId);

    List<SpotHistory> findBySymbolAndCreatedAtBetween(String symbol, LocalDateTime start, LocalDateTime end);
}