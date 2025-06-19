package api.exchange.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.User;
import api.exchange.models.refreshToken;

public interface RefreshTokenRepository extends JpaRepository<refreshToken, Long> {

    refreshToken findByToken(String token);

    Optional<refreshToken> findByUser(User user);

}
