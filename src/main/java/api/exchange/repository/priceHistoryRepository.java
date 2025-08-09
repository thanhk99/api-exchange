package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import api.exchange.models.priceHistoryModel;

@Repository
public interface priceHistoryRepository extends JpaRepository<priceHistoryModel, Long> {
    priceHistoryModel findFirstBySymbolOrderByIdDesc(String symbol);
    priceHistoryModel findTopBySymbolOrderByTimestampDesc(String symbol);
    List<priceHistoryModel> findTop288BySymbolOrderByTimestampDesc(String name);
}
