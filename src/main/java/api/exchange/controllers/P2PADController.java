package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.P2PAd;
import api.exchange.services.P2PADService;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/p2pads")
public class P2PADController {
    @Autowired
    private P2PADService p2padService;

    @PostMapping("create")
    public ResponseEntity<?> createAds(@RequestBody P2PAd p2pAd, @RequestHeader("Authorization") String authHeader) {
        return p2padService.createAddP2P(p2pAd, authHeader);
    }

    @GetMapping("getList")
    public ResponseEntity<?> getListP2PAds() {
        return p2padService.getListP2PAds();
    }

}
