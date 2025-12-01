package api.exchange.repository;

import api.exchange.models.FuturesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesTransactionRepository extends JpaRepository<FuturesTransaction, Long> {
    List<FuturesTransaction> findByUid(String uid);
}
