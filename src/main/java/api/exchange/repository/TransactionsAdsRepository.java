package api.exchange.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.TransactionAds;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface TransactionsAdsRepository extends JpaRepository<TransactionAds, Long> {

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(from_user = :userId OR to_user = :userId) AND status = 'COMPLETED'", nativeQuery = true)
    long countCompletedTransactions(@Param("userId") String userId);
}
