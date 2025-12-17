package api.exchange.repository;

import api.exchange.models.FuturesFundingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesFundingRateRepository extends JpaRepository<FuturesFundingRate, Long> {
    List<FuturesFundingRate> findBySymbolOrderByTimestampDesc(String symbol);
}
