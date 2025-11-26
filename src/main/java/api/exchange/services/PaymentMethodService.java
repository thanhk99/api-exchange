package api.exchange.services;

import api.exchange.models.PaymentMethod;
import api.exchange.repository.PaymentMethodRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.dtos.Request.PaymentMethodRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Add new payment method
     */
    @Transactional
    public ResponseEntity<?> addPaymentMethod(PaymentMethodRequest request, String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setUserId(userId);
        paymentMethod.setType(request.getType());
        paymentMethod.setAccountName(request.getAccountName());
        paymentMethod.setAccountNumber(request.getAccountNumber());
        paymentMethod.setBankName(request.getBankName());
        paymentMethod.setBranchName(request.getBranchName());
        paymentMethod.setQrCode(request.getQrCode());
        paymentMethod.setIsActive(true);

        // If this is set as default, unset other defaults
        if (request.getIsDefault() != null && request.getIsDefault()) {
            paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                    .ifPresent(pm -> {
                        pm.setIsDefault(false);
                        paymentMethodRepository.save(pm);
                    });
            paymentMethod.setIsDefault(true);
        } else {
            // If this is the first payment method, set as default
            long count = paymentMethodRepository.countByUserIdAndIsActiveTrue(userId);
            paymentMethod.setIsDefault(count == 0);
        }

        paymentMethodRepository.save(paymentMethod);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "code", 201,
                        "message", "Payment method added successfully",
                        "data", buildPaymentMethodResponse(paymentMethod)));
    }

    /**
     * Get all user's payment methods
     */
    public ResponseEntity<?> getUserPaymentMethods(String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUserIdAndIsActiveTrue(userId);

        List<Map<String, Object>> data = paymentMethods.stream()
                .map(this::buildPaymentMethodResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", data));
    }

    /**
     * Get payment method by ID
     */
    public ResponseEntity<?> getPaymentMethodById(Long id, String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserIdAndIsActiveTrue(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", buildPaymentMethodResponse(paymentMethod)));
    }

    /**
     * Update payment method
     */
    @Transactional
    public ResponseEntity<?> updatePaymentMethod(Long id, PaymentMethodRequest request, String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserIdAndIsActiveTrue(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        // Update fields
        if (request.getType() != null)
            paymentMethod.setType(request.getType());
        if (request.getAccountName() != null)
            paymentMethod.setAccountName(request.getAccountName());
        if (request.getAccountNumber() != null)
            paymentMethod.setAccountNumber(request.getAccountNumber());
        if (request.getBankName() != null)
            paymentMethod.setBankName(request.getBankName());
        if (request.getBranchName() != null)
            paymentMethod.setBranchName(request.getBranchName());
        if (request.getQrCode() != null)
            paymentMethod.setQrCode(request.getQrCode());

        // Handle default flag
        if (request.getIsDefault() != null && request.getIsDefault() && !paymentMethod.getIsDefault()) {
            // Unset other defaults
            paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                    .ifPresent(pm -> {
                        pm.setIsDefault(false);
                        paymentMethodRepository.save(pm);
                    });
            paymentMethod.setIsDefault(true);
        }

        paymentMethodRepository.save(paymentMethod);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Payment method updated successfully",
                "data", buildPaymentMethodResponse(paymentMethod)));
    }

    /**
     * Soft delete payment method
     */
    @Transactional
    public ResponseEntity<?> deletePaymentMethod(Long id, String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserIdAndIsActiveTrue(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        // Soft delete
        paymentMethod.setIsActive(false);
        paymentMethod.setDeletedAt(LocalDateTime.now());
        paymentMethodRepository.save(paymentMethod);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Payment method deleted successfully"));
    }

    /**
     * Build payment method response
     */
    private Map<String, Object> buildPaymentMethodResponse(PaymentMethod pm) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", pm.getId());
        response.put("type", pm.getType().toString());
        response.put("accountName", pm.getAccountName());
        response.put("accountNumber", pm.getAccountNumber());
        response.put("bankName", pm.getBankName());
        response.put("branchName", pm.getBranchName());
        response.put("qrCode", pm.getQrCode());
        response.put("isDefault", pm.getIsDefault());
        response.put("createdAt", pm.getCreatedAt().toString());
        response.put("updatedAt", pm.getUpdatedAt().toString());
        return response;
    }
}
