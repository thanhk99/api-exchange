package api.exchange.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Response.UserInfoResponse;
import api.exchange.models.User;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> getProfileService(String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.getByUid(userId);
        return ResponseEntity.ok(
                new UserInfoResponse(
                        user.getUid(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getNation()));

    }
}
