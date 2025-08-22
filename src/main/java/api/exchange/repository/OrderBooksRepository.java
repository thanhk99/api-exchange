package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.OrderBooks;

@Repository
public interface OrderBooksRepository extends JpaRepository<OrderBooks,Long> {
    
}
