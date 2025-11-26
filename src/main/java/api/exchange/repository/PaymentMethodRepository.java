package api.exchange.repository;

import api.exchange.models.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Find all active payment methods for a user
    List<PaymentMethod> findByUserIdAndIsActiveTrue(String userId);

    // Find specific payment method by id and userId (active only)
    Optional<PaymentMethod> findByIdAndUserIdAndIsActiveTrue(Long id, String userId);

    // Find default payment method for a user
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(String userId);

    // Count active payment methods for a user
    long countByUserIdAndIsActiveTrue(String userId);
}
