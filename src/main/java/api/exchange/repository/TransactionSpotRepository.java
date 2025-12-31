package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import api.exchange.models.TransactionSpot;

@Repository
public interface TransactionSpotRepository extends JpaRepository<TransactionSpot, Long> {

    // Query methods cho trade history
    List<TransactionSpot> findBySymbol(String symbol, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TransactionSpot t WHERE t.buyerId = :uid OR t.sellerId = :uid")
    List<TransactionSpot> findByUser(@org.springframework.data.repository.query.Param("uid") String uid,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TransactionSpot t WHERE (t.buyerId = :uid OR t.sellerId = :uid) AND t.symbol = :symbol")
    List<TransactionSpot> findByUserAndSymbol(@org.springframework.data.repository.query.Param("uid") String uid,
            @org.springframework.data.repository.query.Param("symbol") String symbol,
            org.springframework.data.domain.Pageable pageable);
}
