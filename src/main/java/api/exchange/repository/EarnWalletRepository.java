package api.exchange.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import api.exchange.models.EarnWallet;

@Repository
public interface EarnWalletRepository extends JpaRepository<EarnWallet, Long> {
    List<EarnWallet> findAllByUid(String uid);

    EarnWallet findByUidAndCurrency(String uid, String currency);
}
