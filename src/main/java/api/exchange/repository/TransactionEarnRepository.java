package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.TransactionEarn;

@Repository
public interface TransactionEarnRepository extends JpaRepository<TransactionEarn, Long> {

    List<TransactionEarn> findByUserId(String userId);

}
