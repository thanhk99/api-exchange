package api.exchange.services;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.dtos.Requset.LoginRequest;
import api.exchange.dtos.Requset.SignupRequest;
import api.exchange.dtos.Response.AuthResponse;
import api.exchange.dtos.Response.RefreshTokenRequest;
import api.exchange.models.User;
import api.exchange.models.UserDevice;
import api.exchange.models.refreshToken;
import api.exchange.repository.RefreshTokenRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

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

    @Autowired
    private DeviceService deviceService;

    public ResponseEntity<?> signup(SignupRequest signupRequest, HttpServletRequest request) {
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

    public ResponseEntity<?> loginService(LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // 1. Xác thực thông tin đăng nhập
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            // 2. Lấy thông tin user
            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already in use"));
            }

            UserDevice device = deviceService.saveDeviceInfo(user, request);

            String accessToken = jwtUtil.generateAccessToken(user);
            refreshToken refreshToken = jwtUtil.generateRefreshToken(user);
            Map<String, Object> deviceRespone = deviceService.buildDeviceResponse(device);
            return ResponseEntity.ok(
                    new AuthResponse(
                            accessToken,
                            refreshToken.getToken(),
                            user.getUid(),
                            user.getEmail(),
                            deviceRespone));

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password", e);
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    @Transactional
    public ResponseEntity<?> RefreshTokenService(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
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
                        user.getEmail(),
                        null));
    }

    public ResponseEntity<?> LogoutService(String authHeader, HttpServletRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid authorization header"));
            }
            String token = authHeader.substring(7);
            int userId = jwtUtil.getUserIdFromToken(token);

            jwtUtil.addToBlacklist(token);
            // 3. Cập nhật trạng thái thiết bị
            deviceService.deactivateDevice(request, userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng xuất thành công",
                    // "deviceId", deviceId,
                    "logoutTime", Instant.now()));

        } catch (Exception e) {
            throw new RuntimeException("Đăng xuất thất bại", e);
        }
    }
}