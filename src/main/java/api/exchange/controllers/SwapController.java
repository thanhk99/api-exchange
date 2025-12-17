package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.dtos.Request.SwapRequest;
import api.exchange.services.SwapService;

@RestController
@RequestMapping("api/v1/swap")
public class SwapController {

    @Autowired
    private SwapService swapService;

    @PostMapping
    public ResponseEntity<?> swapCoin(@RequestHeader("Authorization") String authHeader,
            @RequestBody SwapRequest request) {
        return swapService.swapCoin(authHeader, request);
    }
}
