package api.exchange.repository;

import api.exchange.models.FuturesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesOrderRepository extends JpaRepository<FuturesOrder, Long> {
    List<FuturesOrder> findByUidAndStatus(String uid, FuturesOrder.OrderStatus status);

    List<FuturesOrder> findByUidAndSymbolAndStatus(String uid, String symbol, FuturesOrder.OrderStatus status);
}
