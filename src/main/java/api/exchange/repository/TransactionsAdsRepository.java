package api.exchange.repository;

import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import api.exchange.models.TransactionAds;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.criteria.Predicate;

@Repository
public interface TransactionsAdsRepository extends JpaRepository<TransactionAds, Long> , JpaSpecificationExecutor<TransactionAds> {

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(buyer_id = :userId OR seller_id = :userId) AND status = 'DONE'", nativeQuery = true)
    BigDecimal countCompletedTransactions(@Param("userId") String userId);

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(buyer_id = :userId OR seller_id = :userId) AND status = 'DONE' AND created_at >= :startDate and complete_at <= :endDate ", nativeQuery = true)
    BigDecimal countCompletedTransactions30Days(@Param("userId") String userId ,@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(buyer_id = :userId ) AND status = 'DONE' AND created_at >= :startDate and complete_at <= :endDate ", nativeQuery = true)
    BigDecimal countCompletedBuyTransactions30Days(@Param("userId") String userId ,@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(buyer_id = :userId OR seller_id = :userId) ", nativeQuery = true)
    BigDecimal countTotalTransactions(@Param("userId") String userId);

    @Query(value = "SELECT COUNT(*) FROM transactions_ads WHERE " +
            "(buyer_id = :userId OR seller_id = :userId) AND created_at >= :startDate and complete_at <= :endDate ", nativeQuery = true)
    BigDecimal countTotalTransactions30Days(@Param("userId") String userId , @Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);



    default List<TransactionAds> findByUidWithFilters(String uid, LocalDateTime startDate, 
                                                    LocalDateTime endDate, String status, 
                                                    String tradeType) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            

            if ("BUY".equals(tradeType)) {
                predicates.add(cb.equal(root.get("buyerId"), uid));
            } else if ("SELL".equals(tradeType)) {
                predicates.add(cb.equal(root.get("sellerId"), uid));
            } else {
                predicates.add(cb.or(
                    cb.equal(root.get("buyerId"), uid),
                    cb.equal(root.get("sellerId"), uid)
                ));
            }
            

            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            predicates.add(cb.lessThanOrEqualTo(root.get("completeAt"), endDate));
            

            if (!"ALL".equals(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}
