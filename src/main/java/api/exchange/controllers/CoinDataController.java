package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import api.exchange.services.CoinDataService;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/coin")
public class CoinDataController {

    @Autowired
    private CoinDataService coinDataService;

    @PostMapping("/update-prices")
    public ResponseEntity<?> updatePrices() {
        try {
            coinDataService.fetchAndSaveAllCoinInfo();
            return ResponseEntity.ok(Map.of("message", "success", "data", "Coin prices updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to update prices"));
        }
    }

    @GetMapping("/exchange-rate")
    public ResponseEntity<?> getExchangeRate(@RequestParam String from, @RequestParam String to) {
        try {
            BigDecimal rate = coinDataService.getExchangeRate(from, to);
            return ResponseEntity.ok(Map.of(
                    "from", from.toUpperCase(),
                    "to", to.toUpperCase(),
                    "rate", rate));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to get exchange rate"));
        }
    }

    @GetMapping("/markets")
    public ResponseEntity<?> getMarkets() {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", coinDataService.getAllCoins()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to fetch market data"));
        }
    }

}
