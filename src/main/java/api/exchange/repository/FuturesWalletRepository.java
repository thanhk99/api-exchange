package api.exchange.repository;

import api.exchange.models.FuturesWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FuturesWalletRepository extends JpaRepository<FuturesWallet, Long> {
    Optional<FuturesWallet> findByUidAndCurrency(String uid, String currency);
}
