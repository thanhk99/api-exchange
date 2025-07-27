package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.TransactionFunding;

public interface TransactionFundingRepository extends JpaRepository<TransactionFunding, Long> {

    List<TransactionFunding> findByUserId(String uid);

}
