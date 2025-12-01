package api.exchange.controllers;

import api.exchange.dtos.Request.FuturesOrderRequest;
import api.exchange.dtos.Request.FuturesTransferRequest;
import api.exchange.models.FuturesOrder;
import api.exchange.models.FuturesPosition;
import api.exchange.models.FuturesWallet;
import api.exchange.models.User;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.UserRepository;
import api.exchange.services.FuturesTradingService;
import api.exchange.services.FuturesWalletService;
import api.exchange.services.FuturesMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private UserRepository userRepository;

    @Autowired
    private FuturesMarketDataService futuresMarketDataService;

    private String getUidFromPrincipal(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return userRepository.findByUsername(username)
                .map(User::getUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String uid = getUidFromPrincipal(userDetails);
            FuturesWallet wallet = futuresWalletService.getWallet(uid, "USDT");
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FuturesTransferRequest request) {
        try {
            String uid = getUidFromPrincipal(userDetails);
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

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FuturesOrderRequest request) {
        try {
            String uid = getUidFromPrincipal(userDetails);
            FuturesOrder order = futuresTradingService.placeOrder(
                    uid,
                    request.getSymbol(),
                    request.getSide(),
                    request.getPositionSide(),
                    request.getType(),
                    request.getPrice(),
                    request.getQuantity(),
                    request.getLeverage());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<?> getPositions(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String uid = getUidFromPrincipal(userDetails);
            List<FuturesPosition> positions = futuresPositionRepository.findByUidAndStatus(uid,
                    FuturesPosition.PositionStatus.OPEN);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/position/close")
    public ResponseEntity<?> closePosition(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        try {
            String uid = getUidFromPrincipal(userDetails);
            String symbol = request.get("symbol");
            futuresTradingService.closePosition(uid, symbol);
            return ResponseEntity.ok(Map.of("message", "Position closed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/leverage")
    public ResponseEntity<?> adjustLeverage(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        try {
            String uid = getUidFromPrincipal(userDetails);
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
