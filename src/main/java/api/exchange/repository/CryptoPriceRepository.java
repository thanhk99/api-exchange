package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.CryptoPrice;

public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {
    CryptoPrice findFirstByCryptoIdAndCurrencyOrderByLastUpdatedDesc(
            String cryptoId,
            String currency);
}
