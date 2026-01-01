package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import api.exchange.services.CoinDataService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/coin")
@CrossOrigin(origins = "*")
public class PublicCoinController {

    @Autowired
    private CoinDataService coinDataService;

    /**
     * Get list of all supported coins and their current market data.
     * Replaces: CoinDataController.getMarkets
     */
    @GetMapping("/list")
    public ResponseEntity<?> getCoinList() {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", coinDataService.getAllCoins()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to fetch market data"));
        }
    }

    /**
     * Get exchange rate between two currencies.
     * Replaces: CoinDataController.getExchangeRate
     */
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

}
