package api.exchange.repository;

import api.exchange.models.FuturesPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuturesPositionRepository extends JpaRepository<FuturesPosition, Long> {
    List<FuturesPosition> findByUidAndStatus(String uid, FuturesPosition.PositionStatus status);

    Optional<FuturesPosition> findByUidAndSymbolAndStatus(String uid, String symbol,
            FuturesPosition.PositionStatus status);
}
