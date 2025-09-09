package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.SpotWalletHistory;

public interface SpotWalletHistoryRepository extends JpaRepository<SpotWalletHistory, Long> {

}
