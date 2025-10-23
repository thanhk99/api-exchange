package api.exchange.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import api.exchange.models.User;
import api.exchange.models.refreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<refreshToken, Integer> {

    refreshToken findByToken(String token);

    Optional<refreshToken> findByUser(User user);

    refreshToken findByDeviceId(String deviceId);

}
