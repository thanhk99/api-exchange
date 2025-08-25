package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import api.exchange.models.OrderBooks;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OrderBooksRepository extends JpaRepository<OrderBooks, Long> {

       @Query(value = "SELECT * FROM order_books o WHERE o.symbol = :symbol AND o.order_type = 'BUY' " +
                     "AND o.status IN ('PENDING') " +
                     "ORDER BY o.price DESC, o.created_at ASC", nativeQuery = true)
       List<OrderBooks> findBuyOrderBooks(@Param("symbol") String symbol);

       @Query(value = "SELECT * FROM order_books o WHERE o.symbol = :symbol AND o.order_type = 'SELL' " +
                     "AND o.status IN ('PENDING') " +
                     "ORDER BY o.price ASC, o.created_at ASC", nativeQuery = true)
       List<OrderBooks> findSellOrderBooks(@Param("symbol") String symbol);
}
