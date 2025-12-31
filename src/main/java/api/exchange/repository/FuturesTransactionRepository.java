package api.exchange.repository;

import api.exchange.models.FuturesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesTransactionRepository extends JpaRepository<FuturesTransaction, Long> {
    List<FuturesTransaction> findByUid(String uid);

    // Query methods cho trade history
    List<FuturesTransaction> findByUid(String uid, org.springframework.data.domain.Pageable pageable);

    List<FuturesTransaction> findByUidAndType(String uid, FuturesTransaction.TransactionType type,
            org.springframework.data.domain.Pageable pageable);
}
