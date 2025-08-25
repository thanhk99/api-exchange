package api.exchange.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OrderBooksRepository extends JpaRepository<OrderBooks, Long> {

       // 1. Tìm tất cả active orders cho một symbol (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.status IN ('PENDING')
                     ORDER BY
                         CASE WHEN o.trade_type = 'MARKET' THEN 0 ELSE 1 END,
                         CASE WHEN o.order_type = 'BUY' THEN o.price * -1 ELSE o.price END,
                         o.created_at ASC
                     """, nativeQuery = true)
       List<OrderBooks> findActiveOrders(@Param("symbol") String symbol);

       // 2. Tìm buy orders (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.order_type = 'BUY'
                     AND o.status IN ('PENDING')
                     ORDER BY o.price DESC, o.created_at ASC
                     """, nativeQuery = true)
       List<OrderBooks> findBuyOrderBooks(@Param("symbol") String symbol);

       // 3. Tìm sell orders (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.order_type = 'SELL'
                     AND o.status IN ('PENDING')
                     ORDER BY o.price ASC, o.created_at ASC
                     """, nativeQuery = true)
       List<OrderBooks> findSellOrderBooks(@Param("symbol") String symbol);

       // 4. Tìm market orders (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.trade_type = 'MARKET'
                     AND o.status IN ('PENDING')
                     """, nativeQuery = true)
       List<OrderBooks> findMarketOrders(@Param("symbol") String symbol);

       // 5. Tìm limit orders (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.trade_type = 'LIMIT'
                     AND o.status IN ('PENDING')
                     """, nativeQuery = true)
       List<OrderBooks> findLimitOrders(@Param("symbol") String symbol);

       // 6. Tìm orders theo user (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.uid = :userId
                     AND o.symbol = :symbol
                     ORDER BY o.created_at DESC
                     """, nativeQuery = true)
       List<OrderBooks> findByUserIdAndSymbol(@Param("userId") Long userId, @Param("symbol") String symbol);

       // 7. Tìm orders chưa filled theo price range (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.side = :side
                     AND o.trade_type = 'LIMIT'
                     AND o.status IN ('PENDING')
                     """, nativeQuery = true)
       List<OrderBooks> findOrdersByPriceRange(
                     @Param("symbol") String symbol,
                     @Param("side") String side,
                     @Param("minPrice") BigDecimal minPrice,
                     @Param("maxPrice") BigDecimal maxPrice);

       // 8. Cập nhật order status (Native Query - Custom update)
       @Query(value = """
                     UPDATE order_books
                     SET status = :status,
                         quantity = :quantity,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = :orderId
                     """, nativeQuery = true)
       @Modifying
       @Transactional
       void updateOrderStatusAndQuantity(
                     @Param("orderId") Long orderId,
                     @Param("status") String status,
                     @Param("quantity") BigDecimal quantity);

       // 9. Đếm số active orders (Native Query)
       @Query(value = """
                     SELECT COUNT(*) FROM order_books o
                     WHERE o.symbol = :symbol
                     AND o.status IN ('PENDING')
                     """, nativeQuery = true)
       Long countActiveOrders(@Param("symbol") String symbol);

       // 10. Tìm best bid/ask price (Native Query)
       @Query(value = """
                     SELECT
                         MAX(CASE WHEN order_type = 'BUY' THEN price ELSE NULL END) as best_bid,
                         MIN(CASE WHEN order_type = 'SELL' THEN price ELSE NULL END) as best_ask
                     FROM order_books
                     WHERE symbol = :symbol
                     AND trade_type = 'LIMIT'
                     AND status IN ('PENDING')
                     """, nativeQuery = true)
       Map<String, BigDecimal> findBestBidAsk(@Param("symbol") String symbol);

       // 11. Tìm orders cần expired (Native Query)
       @Query(value = """
                     SELECT * FROM order_books o
                     WHERE o.status = 'PENDING'
                     AND o.created_at < :expiryTime
                     AND o.trade_type = 'LIMIT'
                     """, nativeQuery = true)
       List<OrderBooks> findExpiredOrders(@Param("expiryTime") LocalDateTime expiryTime);

       // 12. Bulk update status (Native Query)
       @Query(value = """
                     UPDATE order_books
                     SET status = :newStatus,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id IN :orderIds
                     AND status = :oldStatus
                     """, nativeQuery = true)
       @Modifying
       @Transactional
       int bulkUpdateOrderStatus(
                     @Param("orderIds") List<Long> orderIds,
                     @Param("oldStatus") String oldStatus,
                     @Param("newStatus") String newStatus);
}
