package api.exchange.controllers;

import api.exchange.services.FuturesMarketDataService;
import api.exchange.services.FuturesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/public/futures")
@CrossOrigin(origins = "*")
public class PublicFuturesController {

    @Autowired
    private FuturesMarketDataService futuresMarketDataService;

    @Autowired
    private FuturesOrderService futuresOrderService;

    @GetMapping("/coins")
    public ResponseEntity<?> getFuturesCoins() {
        try {
            // Get Futures-specific market data (Mark Price, Funding Rate, etc.)
            var futuresMarkets = futuresMarketDataService.getAllFuturesMarkets();
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", futuresMarkets));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/orders/orderbook/{symbol}")
    public ResponseEntity<?> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            var orderBook = futuresOrderService.getOrderBook(symbol, limit);
            return ResponseEntity.ok(Map.of(
                    "message", "Success",
                    "data", orderBook));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
