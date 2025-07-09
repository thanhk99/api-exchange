package api.exchange.services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Requset.PasswordRequest;
import api.exchange.models.User;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;

@Service
public class PasswordService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public ResponseEntity<?> changePassLv2Service(String authHeader, PasswordRequest request) {
        // Validate token and extract user ID
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "INVALID_AUTH_HEADER", "Authorization header is invalid");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "INVALID_TOKEN", "Token is not valid");
        }

        // Get user information
        UUID uid = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.getByUid(uid);
        if (user == null) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        // Validate password requirements
        if (request.getNewLv2Password() == null || request.getNewLv2Password().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "EMPTY_PASSWORD", "New password cannot be empty");
        }

        // Handle password change logic
        try {
            if (user.getPasswordLevel2() == null) {
                // First-time password setup
                user.setPasswordLevel2(request.getNewLv2Password());
            } else {
                // Existing password change
                if (!user.getPasswordLevel2().equals(request.getOldLv2Password())) {
                    return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_OLD_PASSWORD",
                            "Current level 2 password is incorrect");
                }
                user.setPasswordLevel2(request.getNewLv2Password());
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password changed successfully"));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR",
                    "An error occurred while changing password");
        }
    }

    @Transactional
    public ResponseEntity<?> changePassService(String authHeader, PasswordRequest request) {
        // Validate token and extract user ID
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "INVALID_AUTH_HEADER", "Authorization header is invalid");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "INVALID_TOKEN", "Token is not valid");
        }

        // Get user information
        UUID uid = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.getByUid(uid);
        if (user == null) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        // Validate password requirements
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "EMPTY_PASSWORD", "New password cannot be empty");
        }

        try {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_OLD_PASSWORD",
                        "Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password changed successfully"));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR",
                    "An error occurred while changing password");
        }
    }

    // Helper method for consistent error responses
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String errorCode,
            String message) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "status", "ERROR",
                        "code", errorCode,
                        "message", message));
    }
}
