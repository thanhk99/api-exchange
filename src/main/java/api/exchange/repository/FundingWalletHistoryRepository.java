package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.FundingWalletHistory;

public interface FundingWalletHistoryRepository extends JpaRepository<FundingWalletHistory, Long> {

    List<FundingWalletHistory> findByUserId(String uid);

    boolean existsByNoteContaining(String note);

    FundingWalletHistory findFirstByNoteContaining(String note);
}
