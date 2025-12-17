package api.exchange.controllers;

import api.exchange.dtos.Request.PaymentMethodRequest;
import api.exchange.services.PaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    @PostMapping
    public ResponseEntity<?> addPaymentMethod(
            @RequestBody PaymentMethodRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return paymentMethodService.addPaymentMethod(request, authHeader);
    }

    @GetMapping
    public ResponseEntity<?> getUserPaymentMethods(@RequestHeader("Authorization") String authHeader) {
        return paymentMethodService.getUserPaymentMethods(authHeader);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentMethodById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return paymentMethodService.getPaymentMethodById(id, authHeader);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return paymentMethodService.updatePaymentMethod(id, request, authHeader);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaymentMethod(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return paymentMethodService.deletePaymentMethod(id, authHeader);
    }
}
