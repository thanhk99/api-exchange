package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.exchange.models.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User findByUid(String userId);

    User findByPhone(String phone);

    Optional<User> findByUsername(String username);
}