package api.exchange.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Response.UserFullInfoResponse;
import api.exchange.dtos.Response.UserInfoResponse;
import api.exchange.models.User;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> getProfileService(String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findByUid(userId);
        return ResponseEntity.ok(Map.of("message", "success", "data",
                new UserInfoResponse(
                        user.getUid(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getNation(),
                        user.getKycStatus(),
                        user.getPhone(),
                        user.getUserLevel(),
                        user.getUserStatus())));

    }

    public ResponseEntity<?> changeName(String username, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);
            User userInfo = userRepository.findByUid(uid);

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "USERNAME_INVALID"));
            }

            userInfo.setUsername(username);
            userRepository.save(userInfo);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "ERROR_SERVER"));
        }
    }

    public ResponseEntity<?> getAllinfo(String header) {
        String token = header.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findByUid(userId);
        return ResponseEntity.ok(Map.of("message", "success", "data", new UserFullInfoResponse(
                user.getUid(),
                user.getEmail(),
                user.getUsername(),
                user.getUserStatus(),
                user.getNation(),
                user.getKycStatus(),
                user.getUserLevel(),
                user.getPhone(),
                "Nguời dùng thông thường ")));
    }

    @Transactional
    public ResponseEntity<?> changePhone(String phone, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);
            User userInfo = userRepository.findByUid(uid);

            // Validate phone number
            if (phone == null || phone.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "PHONE_INVALID"));
            }

            // Check if phone number already exists for another user
            User existingUser = userRepository.findByPhone(phone);
            if (existingUser != null && !existingUser.getUid().equals(uid)) {
                return ResponseEntity.badRequest().body(Map.of("message", "PHONE_ALREADY_EXISTS"));
            }

            userInfo.setPhone(phone);
            userRepository.save(userInfo);
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("message", "ERROR_SERVER"));
        }
    }

}
