package api.exchange.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import api.exchange.dtos.Request.LoginRequest;
import api.exchange.dtos.Request.RefreshTokenRequest;
import api.exchange.dtos.Request.SignupRequest;
import api.exchange.dtos.Response.AuthResponse;
import api.exchange.models.User;
import api.exchange.models.UserDevice;
import api.exchange.models.refreshToken;
import api.exchange.repository.RefreshTokenRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest, HttpServletRequest request) {
        try {
            // Validate dữ liệu đầu vào
            if (signupRequest.getEmail() == null || signupRequest.getEmail().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error", "VALIDATION_ERROR",
                                "message", "Email không được để trống",
                                "field", "email"));
            }

            if (signupRequest.getPassword() == null || signupRequest.getPassword().length() < 6) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error", "VALIDATION_ERROR",
                                "message", "Mật khẩu phải có ít nhất 6 ký tự",
                                "field", "password"));
            }

            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT) // 409 Conflict
                        .body(Map.of(
                                "error", "EMAIL_EXISTS",
                                "message", "Email đã được sử dụng",
                                "field", "email"));
            }

            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setCreatedAt(LocalDateTime.now());
            user.setNation(signupRequest.getNation());

            User savedUser = userRepository.save(user);

            return ResponseEntity
                    .status(HttpStatus.CREATED) // 201 Created
                    .body(Map.of(
                            "success", true,
                            "message", "Đăng ký thành công",
                            "data", Map.of(
                                    "id", savedUser.getUid(),
                                    "username", savedUser.getUsername(),
                                    "email", savedUser.getEmail())));

        } catch (Exception e) {
            // Xử lý lỗi hệ thống
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "error", "SERVER_ERROR",
                            "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> loginService(LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // Xác thực thông tin đăng nhập
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            // Lấy thông tin user
            User user = userRepository.findByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already in use"));
            }

            UserDevice device = deviceService.saveDeviceInfo(user, request);
            String deviceId = device.getDeviceId();
            LocalDateTime lastLogin = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            user.setLastLogin(lastLogin);
            String accessToken = jwtUtil.generateAccessToken(user);
            refreshToken refreshToken = jwtUtil.generateRefreshToken(user, deviceId);
            Map<String, Object> deviceRespone = deviceService.buildDeviceResponse(device);

            return ResponseEntity.ok(Map.of("message", "success", "data",
                    new AuthResponse(
                            accessToken,
                            refreshToken.getToken(),
                            user.getUid(),
                            user.getEmail(),
                            deviceRespone)));

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "LOGIN_ERROR",
                            "message", "Tài khoản mật khẩu không đúng"));
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    @Transactional
    public ResponseEntity<?> RefreshTokenService(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        try {
            String requestRefreshToken = refreshTokenRequest.getRefreshToken();
            if (requestRefreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            refreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken);
            String deviceId = refreshToken.getDeviceId();
            if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                refreshTokenRepository.delete(refreshToken);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "refresh token expried"));
            }
            refreshTokenRepository.delete(refreshToken);

            // Tạo token mới
            User user = refreshToken.getUser();
            String newAccessToken = jwtUtil.generateAccessToken(user);
            refreshToken newRefreshToken = jwtUtil.generateRefreshToken(user, deviceId);

            // Trả về response
            return ResponseEntity.ok(
                    new AuthResponse(
                            newAccessToken,
                            newRefreshToken.getToken(),
                            user.getUid(),
                            user.getEmail(),
                            null));

        } catch (ResponseStatusException e) {
            throw e; // Re-throw các lỗi đã được định nghĩa
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "refresh token expried"));
        }
    }

    public ResponseEntity<?> revokeDevice(String authHeader, UserDevice userDevice) {
        try {
            String device_id = userDevice.getDeviceId();
            String token = authHeader.substring(7);
            jwtUtil.addToBlacklist(token);
            // Cập nhật trạng thái thiết bị
            deviceService.deactivateDevice(device_id);

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng xuất thành công",
                    "logoutTime", Instant.now()));

        } catch (Exception e) {
            throw new RuntimeException("Đăng xuất thất bại", e);
        }
    }

    public ResponseEntity<?> isExistEmail(SignupRequest signupRequest) {
        try {
            User isExisit = userRepository.findByEmail(signupRequest.getEmail());
            if (isExisit != null) {
                return ResponseEntity.ok(Map.of("message", "email đã tồn tại"));
            } else {
                return ResponseEntity.ok(Map.of("message", "success"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "SERVER_ERROR"));
        }
    }
}