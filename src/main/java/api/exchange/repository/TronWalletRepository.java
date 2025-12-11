package api.exchange.repository;

import api.exchange.models.TronWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TronWalletRepository extends JpaRepository<TronWallet, Long> {
    Optional<TronWallet> findByUid(String uid);

    Optional<TronWallet> findByAddress(String address);
}
