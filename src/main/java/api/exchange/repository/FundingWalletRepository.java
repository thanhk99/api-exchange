package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.FundingWallet;

@Repository
public interface FundingWalletRepository extends JpaRepository<FundingWallet, Long> {

    FundingWallet findByUid(String uid);

    FundingWallet findByUidAndCurrency(String uid, String currency);

}
