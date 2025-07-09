package api.exchange.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
