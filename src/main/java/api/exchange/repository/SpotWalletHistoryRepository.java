package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.SpotWalletHistory;

public interface SpotWalletHistoryRepository extends JpaRepository<SpotWalletHistory, Long> {

    List<SpotWalletHistory> findByUserId(String userId);

}
