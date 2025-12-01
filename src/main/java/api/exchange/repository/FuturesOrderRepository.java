package api.exchange.repository;

import api.exchange.models.FuturesOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesOrderRepository extends JpaRepository<FuturesOrder, Long> {
    List<FuturesOrder> findByUidAndStatus(String uid, FuturesOrder.OrderStatus status);

    List<FuturesOrder> findByUidAndSymbolAndStatus(String uid, String symbol, FuturesOrder.OrderStatus status);

    // Get all orders for a user, ordered by creation time
    List<FuturesOrder> findByUidOrderByCreatedAtDesc(String uid, Pageable pageable);

    // Get orders filtered by symbol
    List<FuturesOrder> findByUidAndSymbolOrderByCreatedAtDesc(String uid, String symbol, Pageable pageable);

    // Get orders filtered by status
    List<FuturesOrder> findByUidAndStatusOrderByCreatedAtDesc(String uid, FuturesOrder.OrderStatus status,
            Pageable pageable);

    // Get orders filtered by symbol and status
    List<FuturesOrder> findByUidAndSymbolAndStatusOrderByCreatedAtDesc(
            String uid, String symbol, FuturesOrder.OrderStatus status, Pageable pageable);

    // For order book - get pending limit orders by symbol and side
    List<FuturesOrder> findBySymbolAndSideAndStatusAndType(
            String symbol,
            FuturesOrder.OrderSide side,
            FuturesOrder.OrderStatus status,
            FuturesOrder.OrderType type,
            Pageable pageable);
}
