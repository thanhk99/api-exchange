package api.exchange.controllers;

import api.exchange.models.FuturesPosition;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.sercurity.services.AuthenticationService;
import api.exchange.services.FuturesTradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/futures/positions")
@CrossOrigin(origins = "*")
public class FuturesPositionController {

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private FuturesTradingService futuresTradingService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getPositions() {
        try {
            String uid = authenticationService.getCurrentUserId();
            List<FuturesPosition> positions = futuresPositionRepository.findByUidAndStatus(uid,
                    FuturesPosition.PositionStatus.OPEN);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/close")
    public ResponseEntity<?> closePosition(@RequestBody Map<String, String> request) {
        try {
            String uid = authenticationService.getCurrentUserId();
            String symbol = request.get("symbol");
            futuresTradingService.closePosition(uid, symbol);
            return ResponseEntity.ok(Map.of("message", "Position closed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/leverage")
    public ResponseEntity<?> adjustLeverage(@RequestBody Map<String, Object> request) {
        try {
            String uid = authenticationService.getCurrentUserId();
            String symbol = (String) request.get("symbol");
            int leverage = (int) request.get("leverage");
            futuresTradingService.adjustLeverage(uid, symbol, leverage);
            return ResponseEntity.ok(Map.of("message", "Leverage adjusted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
