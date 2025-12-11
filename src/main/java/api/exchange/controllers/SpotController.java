package api.exchange.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.OrderBooks;
import api.exchange.services.SpotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("api/v1/spot")
public class SpotController {

    @Autowired
    private SpotService spotService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderBooks entity,
            @RequestHeader("Authorization") String header) {
        return spotService.createOrder(entity, header);
    }

    @PostMapping("/cancle")
    public ResponseEntity<?> cancleOrder(@RequestBody OrderBooks entity) {
        return spotService.cancleOrder(entity.getId());
    }

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<?> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            var orderBook = spotService.getOrderBook(symbol, limit);
            return ResponseEntity.ok(orderBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
