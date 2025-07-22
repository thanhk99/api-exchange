package api.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.P2PAd;
import api.exchange.models.User;

public interface P2PAdRepository extends JpaRepository<P2PAd, Long> {

    List<P2PAd> findByUserId(String uid);

}
