package api.exchange.services;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Requset.LoginRequest;
import api.exchange.dtos.Requset.SignupRequest;
import api.exchange.dtos.Response.AuthResponse;
import api.exchange.dtos.Response.RefreshTokenRequest;
import api.exchange.models.User;
import api.exchange.models.refreshToken;
import api.exchange.repository.RefreshTokenRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email đã được sử dụng"));
        }

        // Tạo user mới
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRoles("USER");

        // Lưu user vào database
        User savedUser = userRepository.save(user);

        // Tạo JWT token
        String accessToken = jwtUtil.generateAccessToken(savedUser);
        refreshToken refreshToken = jwtUtil.generateRefreshToken(savedUser);

        // Trả về response
        return ResponseEntity.ok(Map.of(
                "message", "Đăng ký thành công",
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "user", Map.of(
                        "id", savedUser.getUid(),
                        "username", savedUser.getUsername(),
                        "email", savedUser.getEmail())));
    }

    public ResponseEntity<?> loginService(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user != null) {
            new RuntimeException("User not found");
        }
        String accessToken = jwtUtil.generateAccessToken(user);
        refreshToken refreshToken = jwtUtil.generateRefreshToken(user);

        return ResponseEntity.ok(
                new AuthResponse(
                        accessToken,
                        refreshToken.getToken(),
                        user.getUid(),
                        user.getEmail()));
    }

    public ResponseEntity<?> RefreshTokenService(RefreshTokenRequest refreshTokenRequest) {
        refreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken());
        if (refreshToken == null) {
            new RuntimeException("Refresh Token Invalid");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh Token was expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        refreshToken newRefreshToken = jwtUtil.generateRefreshToken(user);

        refreshTokenRepository.delete(refreshToken);

        return ResponseEntity.ok(
                new AuthResponse(
                        newAccessToken,
                        newRefreshToken.getToken(),
                        user.getUid(),
                        user.getEmail()));
    }

    public ResponseEntity<?> LogoutService(String authHeader) {
        String token = authHeader.substring(7);
        jwtUtil.addToBlacklist(token);
        return ResponseEntity.ok("Logout successful");
    }
}
