package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import api.exchange.models.SpotWallet;
import api.exchange.services.SpotWalletService;


@RestController
@RequestMapping("/api/v1/spot-wallet")
public class SpotWalletController {
    
    @Autowired 
    private SpotWalletService spotWalletService;

    @PostMapping("/addBalance")
    public ResponseEntity<?> addBalance(@RequestBody SpotWallet spotWallet) {
        return spotWalletService.addBalanceCoin(spotWallet);
    }
    
}
