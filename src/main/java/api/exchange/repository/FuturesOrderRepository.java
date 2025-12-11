package api.exchange.repository;

import api.exchange.models.FuturesOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

        // For order book - get pending and partially filled limit orders by symbol and
        // side
        List<FuturesOrder> findBySymbolAndSideAndStatusInAndType(
                        String symbol,
                        FuturesOrder.OrderSide side,
                        List<FuturesOrder.OrderStatus> statuses,
                        FuturesOrder.OrderType type,
                        Pageable pageable);

        // Public queries (no uid filter)
        List<FuturesOrder> findBySymbolAndStatusOrderByCreatedAtDesc(String symbol, FuturesOrder.OrderStatus status,
                        Pageable pageable);

        List<FuturesOrder> findBySymbolOrderByCreatedAtDesc(String symbol, Pageable pageable);

        List<FuturesOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

        // Self-Trade Prevention checks
        boolean existsByUidAndSymbolAndSideAndStatusAndPriceLessThanEqual(
                        String uid, String symbol, FuturesOrder.OrderSide side, FuturesOrder.OrderStatus status,
                        BigDecimal price);

        boolean existsByUidAndSymbolAndSideAndStatusAndPriceGreaterThanEqual(
                        String uid, String symbol, FuturesOrder.OrderSide side, FuturesOrder.OrderStatus status,
                        BigDecimal price);

        boolean existsByUidAndSymbolAndSideAndStatusInAndPriceLessThanEqual(
                        String uid, String symbol, FuturesOrder.OrderSide side, List<FuturesOrder.OrderStatus> statuses,
                        BigDecimal price);

        boolean existsByUidAndSymbolAndSideAndStatusInAndPriceGreaterThanEqual(
                        String uid, String symbol, FuturesOrder.OrderSide side, List<FuturesOrder.OrderStatus> statuses,
                        BigDecimal price);

        // Matching Engine Queries
        // Buy Orders: Highest Price First, then Oldest First
        List<FuturesOrder> findBySymbolAndSideAndStatusOrderByPriceDescCreatedAtAsc(
                        String symbol, FuturesOrder.OrderSide side, FuturesOrder.OrderStatus status);

        // Sell Orders: Lowest Price First, then Oldest First
        List<FuturesOrder> findBySymbolAndSideAndStatusOrderByPriceAscCreatedAtAsc(
                        String symbol, FuturesOrder.OrderSide side, FuturesOrder.OrderStatus status);

        // Buy Orders: Highest Price First, then Oldest First (Multiple Statuses)
        List<FuturesOrder> findBySymbolAndSideAndStatusInOrderByPriceDescCreatedAtAsc(
                        String symbol, FuturesOrder.OrderSide side, List<FuturesOrder.OrderStatus> statuses);

        // Sell Orders: Lowest Price First, then Oldest First (Multiple Statuses)
        List<FuturesOrder> findBySymbolAndSideAndStatusInOrderByPriceAscCreatedAtAsc(
                        String symbol, FuturesOrder.OrderSide side, List<FuturesOrder.OrderStatus> statuses);
}
