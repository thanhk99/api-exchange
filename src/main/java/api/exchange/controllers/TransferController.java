package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.dtos.Request.InternalTransferRequest;
import api.exchange.dtos.Request.WalletTransferRequest;
import api.exchange.services.TransferService;

@RestController
@RequestMapping("api/v1/transfer")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @PostMapping("/internal")
    public ResponseEntity<?> internalTransfer(@RequestHeader("Authorization") String authHeader,
            @RequestBody InternalTransferRequest request) {
        return transferService.internalTransfer(authHeader, request);
    }

    @PostMapping("/wallet")
    public ResponseEntity<?> walletTransfer(@RequestHeader("Authorization") String authHeader,
            @RequestBody WalletTransferRequest request) {
        return transferService.walletTransfer(authHeader, request);
    }
}
