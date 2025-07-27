package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

}
