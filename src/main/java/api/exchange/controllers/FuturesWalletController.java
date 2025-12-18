package api.exchange.controllers;

import api.exchange.dtos.Request.FuturesTransferRequest;
import api.exchange.dtos.Response.FuturesWalletResponse;
import api.exchange.sercurity.services.AuthenticationService;
import api.exchange.services.FuturesWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/futures/wallet")
@CrossOrigin(origins = "*")
public class FuturesWalletController {

    @Autowired
    private FuturesWalletService futuresWalletService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        try {
            String uid = authenticationService.getCurrentUserId();
            FuturesWalletResponse walletInfo = futuresWalletService.getWalletInfo(uid, "USDT");
            return ResponseEntity.ok(walletInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody FuturesTransferRequest request) {
        try {
            String uid = authenticationService.getCurrentUserId();
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
}
