package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import api.exchange.services.CoinDataService;
import java.util.Map;

@RestController
@RequestMapping("api/v1/coin")
public class CoinDataController {

    @Autowired
    private CoinDataService coinDataService;

    @org.springframework.web.bind.annotation.GetMapping("/markets")
    public ResponseEntity<?> getMarkets() {
        try {
            return ResponseEntity.ok(Map.of("message", "success", "data", coinDataService.getAllCoins()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to fetch market data"));
        }
    }

    @PostMapping("/update-prices")
    public ResponseEntity<?> updatePrices() {
        try {
            coinDataService.fetchAndSaveAllCoinInfo();
            return ResponseEntity.ok(Map.of("message", "success", "data", "Coin prices updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to update prices"));
        }
    }

}
