package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);

}
