package api.exchange.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.OrderBooks;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface OrderBooksRepository extends JpaRepository<OrderBooks,Long> {
    @Query("SELECT o FROM order_books o WHERE o.symbol = :symbol AND o.orderType = 'BUY' " +
           "AND o.status IN ('PENDING') " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<OrderBooks> findBuyOrders(@Param("symbol") String symbol);
    
    @Query("SELECT o FROM order_books o WHERE o.symbol = :symbol AND o.orderType = 'SELL' " +
           "AND o.status IN ('PENDING') " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<OrderBooks> findSellOrders(@Param("symbol") String symbol);
}
