package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.priceHistoryModel;

@Repository
public interface priceHistoryRepository extends JpaRepository<priceHistoryModel, Long> {
    priceHistoryModel findFirstBySymbolOrderByIdDesc(String symbol);
}
