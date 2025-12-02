package api.exchange.controllers;

import api.exchange.dtos.Request.FuturesTransferRequest;
import api.exchange.dtos.Response.FuturesWalletResponse;
import api.exchange.models.FuturesPosition;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.services.FuturesTradingService;
import api.exchange.services.FuturesWalletService;
import api.exchange.services.FuturesMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/futures")
public class FuturesController {

    @Autowired
    private FuturesWalletService futuresWalletService;

    @Autowired
    private FuturesTradingService futuresTradingService;

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private FuturesMarketDataService futuresMarketDataService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractUid(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            String uid = extractUid(authHeader);
            FuturesWalletResponse walletInfo = futuresWalletService.getWalletInfo(uid, "USDT");
            return ResponseEntity.ok(walletInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestHeader("Authorization") String authHeader,
            @RequestBody FuturesTransferRequest request) {
        try {
            String uid = extractUid(authHeader);
            if ("TO_FUTURES".equalsIgnoreCase(request.getType())) {
                futuresWalletService.transferToFutures(uid, request.getAmount());
            } else {
                futuresWalletService.transferFromFutures(uid, request.getAmount());
            }
            return ResponseEntity.ok(Map.of("message", "Transfer successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<?> getPositions(@RequestHeader("Authorization") String authHeader) {
        try {
            String uid = extractUid(authHeader);
            List<FuturesPosition> positions = futuresPositionRepository.findByUidAndStatus(uid,
                    FuturesPosition.PositionStatus.OPEN);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/position/close")
    public ResponseEntity<?> closePosition(@RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String uid = extractUid(authHeader);
            String symbol = request.get("symbol");
            futuresTradingService.closePosition(uid, symbol);
            return ResponseEntity.ok(Map.of("message", "Position closed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/leverage")
    public ResponseEntity<?> adjustLeverage(@RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String uid = extractUid(authHeader);
            String symbol = (String) request.get("symbol");
            int leverage = (int) request.get("leverage");
            futuresTradingService.adjustLeverage(uid, symbol, leverage);
            return ResponseEntity.ok(Map.of("message", "Leverage adjusted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

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
}
