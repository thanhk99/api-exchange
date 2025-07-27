package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import api.exchange.models.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
