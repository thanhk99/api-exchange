package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import api.exchange.models.SpotWallet;

public interface SpotWalletRepository extends JpaRepository<SpotWallet, Long> {

    SpotWallet findByUidAndCurrency(String uid, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM SpotWallet w WHERE w.uid = :uid AND w.currency = :currency")
    SpotWallet findByUidAndCurrencyWithLock(@Param("uid") String uid, @Param("currency") String currency);

    List<SpotWallet> findAllByUid(String uid);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
    void advisoryLock(@Param("lockKey") String lockKey);
}