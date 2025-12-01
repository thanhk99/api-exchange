package api.exchange.repository;

import api.exchange.models.FuturesCoinData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuturesCoinDataRepository extends JpaRepository<FuturesCoinData, String> {
    List<FuturesCoinData> findAllByOrderByVolume24hDesc();
}
