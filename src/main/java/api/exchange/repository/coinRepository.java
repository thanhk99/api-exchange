package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.coinModel;

@Repository
public interface coinRepository extends JpaRepository<coinModel, String> {

}
