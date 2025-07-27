package api.exchange.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.CryptoPrice;
import api.exchange.services.P2PCoinService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/v1/p2p")
public class P2PCoinController {

    @Autowired
    private P2PCoinService p2pCoinService;

    @PostMapping("getCoin")
    public ResponseEntity<?> getCryptoRates(@RequestBody CryptoPrice entity) {
        CryptoPrice cryptoPrice = p2pCoinService.getCryptoRates(entity);
        if (cryptoPrice == null) {
            return ResponseEntity.badRequest().body(Map.of("ERROR", "INVALID", "message", "crypto not exist"));
        }
        return ResponseEntity.ok(cryptoPrice);

    }

}
