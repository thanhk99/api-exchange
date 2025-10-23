package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.TransactionSpot;

@Repository
public interface TransactionSpotRepository extends JpaRepository<TransactionSpot,Long>{
    
}
