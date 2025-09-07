package api.exchange.services;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import api.exchange.models.CryptoPrice;
import api.exchange.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class P2PCoinService {

        @Autowired
        private CryptoPriceRepository cryptoPriceRepository;

        private final RestTemplate restTemplate;

        private static final List<String> CRYPTO_IDS = Arrays.asList("bitcoin", "ethereum", "tether", "usd-coin",
                        "notcoin");

        // Danh sách các loại tiền fiat (10 loại)
        private static final List<String> FIAT_CURRENCIES = Arrays.asList("usd", "vnd", "eur", "jpy", "gbp", "aud",
                        "cad",
                        "sgd", "cny", "krw");

        @Cacheable(value = "cryptoPrices", key = "#cryptoPrice.cryptoId + '-' + #cryptoPrice.currency", unless = "#result == null")
        public CryptoPrice getCryptoRates(CryptoPrice cryptoPrice) {
                CryptoPrice cryptoPriceInfo = cryptoPriceRepository
                                .findFirstByCryptoIdAndCurrencyOrderByLastUpdatedDesc(
                                                cryptoPrice.getCryptoId(), cryptoPrice.getCurrency());
                return cryptoPriceInfo;
        }

        // @CacheEvict(value = "cryptoPrices", allEntries = true)
        // @Scheduled(fixedRate = 300000)
        // public void updateCryptoPrices() {
        // for (String cryptoId : CRYPTO_IDS) {
        // String url = coingeckoApiUrl + "/simple/price?ids=" + cryptoId +
        // "&vs_currencies="
        // + String.join(",", FIAT_CURRENCIES);
        // Map<String, Map<String, Double>> response = restTemplate.getForObject(url,
        // Map.class);

        // if (response != null && response.containsKey(cryptoId)) {
        // Map<String, Double> rates = response.get(cryptoId);
        // for (String currency : rates.keySet()) {
        // Number price = (Number) rates.get(currency);
        // CryptoPrice cryptoPrice = new CryptoPrice();
        // cryptoPrice.setCryptoId(cryptoId);
        // cryptoPrice.setCurrency(currency);
        // cryptoPrice.setPrice(BigDecimal.valueOf(price.doubleValue()));
        // cryptoPrice.setLastUpdated(LocalDateTime.now());
        // cryptoPriceRepository.save(cryptoPrice);
        // }
        // }
        // }
        // }
}
