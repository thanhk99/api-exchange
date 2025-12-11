package api.exchange.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.User;
import api.exchange.models.UserDevice;
import api.exchange.models.refreshToken;
import api.exchange.repository.RefreshTokenRepository;
import api.exchange.repository.UserDeviceRepository;
import api.exchange.repository.UserRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

@Service
public class DeviceService {

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public UserDevice saveDeviceInfo(User user, HttpServletRequest request) {
        // Tạo deviceId mới nếu chưa có từ client
        String userAgent = request.getHeader("User-Agent");
        Parser uaParser = new Parser();
        Client client = uaParser.parse(userAgent);
        String deviceId = Optional.ofNullable(request.getHeader("Device-Id"))
                .orElse(UUID.randomUUID().toString());
        LocalDateTime timeNow = LocalDateTime.now();
        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(extractDeviceName(request))
                .deviceType(detectDeviceType(request))
                .ipAddress(request.getRemoteAddr())
                .location(extractLocationFromIp(request.getRemoteAddr()))
                .browserName(client.userAgent.family)
                .lastLoginAt(timeNow)
                .isActive(true)
                .build();
        userDeviceRepository.save(device);
        return device;
    }

    private String extractDeviceName(HttpServletRequest request) {
        // Ưu tiên lấy từ header Device-Name nếu có
        String deviceName = request.getHeader("Device-Name");
        if (deviceName != null && !deviceName.isEmpty()) {
            return deviceName;
        }

        // Fallback: Lấy từ User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.contains("Android"))
            return "Android Device";
        if (userAgent.contains("iPhone"))
            return "iPhone";
        if (userAgent.contains("iPad"))
            return "iPad";
        if (userAgent.contains("Windows"))
            return "Windows PC";
        if (userAgent.contains("Macintosh"))
            return "Mac";

        return "Unknown Device";
    }

    private String detectDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent").toLowerCase();

        if (userAgent.contains("mobile")) {
            return "MOBILE";
        } else if (userAgent.contains("tablet")) {
            return "TABLET";
        } else if (userAgent.contains("android") || userAgent.contains("iphone")) {
            return "MOBILE";
        } else {
            return "DESKTOP";
        }
    }

    private String extractLocationFromIp(String ipAddress) {
        // Triển khai thực tế sẽ gọi API geolocation
        // Ở đây chỉ là mock data
        if (ipAddress.startsWith("192.168.") || ipAddress.equals("127.0.0.1")) {
            return "Local Network";
        }
        return "Unknown Location";
    }

    public Map<String, Object> buildDeviceResponse(UserDevice device) {
        return Map.of(
                "deviceId", device.getDeviceId(),
                "deviceName", device.getDeviceName(),
                "deviceType", device.getDeviceType(),
                "ipAddress", device.getIpAddress(),
                "location", device.getLocation(),
                "browser", device.getBrowserName(),
                "lastLogin", device.getLastLoginAt());
    }

    @Transactional
    public void deactivateDevice(String deviceId) {
        LocalDateTime timeNow = LocalDateTime.now();
        userDeviceRepository.findByDeviceId(deviceId)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setLogoutAt(timeNow);
                    userDeviceRepository.save(device);
                });
    }

    public String extractDeviceId(HttpServletRequest request) {
        // Ưu tiên lấy từ header
        String deviceId = request.getHeader("Device-Id");

        // Fallback: lấy từ parameter hoặc attribute
        if (deviceId == null) {
            deviceId = request.getParameter("deviceId");
        }

        // Final fallback: nếu không có thì không xử lý (tuỳ logic nghiệp vụ)
        return deviceId;
    }

    public ResponseEntity<?> getListDevice(String authHeader) {
        try {
            // Extract and validate token
            String token = authHeader.substring(7);
            // Get user information
            String uid = jwtUtil.getUserIdFromToken(token);
            User user = userRepository.findByUid(uid);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "ERROR",
                                "code", "USER_NOT_FOUND",
                                "message", "User not found"));
            }

            List<UserDevice> userDevices = userDeviceRepository.findByUser_UidAndIsActive(uid, true);
            if (userDevices == null || userDevices.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> deviceResponses = new ArrayList<>();
            for (UserDevice userDevice : userDevices) {
                Map<String, Object> deviceResponse = buildDeviceResponse(userDevice);

                // Add more fields as needed
                deviceResponses.add(deviceResponse);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", deviceResponses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "code", "SERVER_ERROR",
                            "message", "An unexpected error occurred"));
        }
    }

    @Transactional
    public ResponseEntity<?> revokeDevice(String deviceId, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUserIdFromToken(token);

            UserDevice userDevice = userDeviceRepository.findByDeviceId(deviceId).orElse(null);

            if (userDevice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Device not found"));
            }

            // Verify ownership
            if (!userDevice.getUser().getUid().equals(uid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Access denied"));
            }

            if (userDevice.isActive()) {
                LocalDateTime timeNow = LocalDateTime.now();
                userDevice.setActive(false);
                userDevice.setLogoutAt(timeNow);
                userDeviceRepository.save(userDevice);

                // Delete associated refresh token if exists
                refreshToken refreshToken = refreshTokenRepository.findByDeviceId(deviceId);
                if (refreshToken != null) {
                    refreshTokenRepository.delete(refreshToken);
                }

                return ResponseEntity.ok(Map.of("message", "success", "data", "Revoke device success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Device is already revoked"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred"));
        }
    }
}