package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.SpotWallet;

public interface SpotWalletRepository extends JpaRepository<SpotWallet, Long> {

    SpotWallet findByUidAndCurrency(String uid, String currency);

    List<SpotWallet> findAllByUid(String uid);

}