package api.exchange.services;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import api.exchange.models.User;
import api.exchange.models.UserDevice;
import api.exchange.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

@Service
public class DeviceService {

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    public UserDevice saveDeviceInfo(User user, HttpServletRequest request) {
        // Tạo deviceId mới nếu chưa có từ client
        String userAgent = request.getHeader("User-Agent");
        Parser uaParser = new Parser();
        Client client = uaParser.parse(userAgent);
        String deviceId = Optional.ofNullable(request.getHeader("Device-Id"))
                .orElse(UUID.randomUUID().toString());

        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(extractDeviceName(request))
                .deviceType(detectDeviceType(request))
                .ipAddress(request.getRemoteAddr())
                .location(extractLocationFromIp(request.getRemoteAddr()))
                .browserName(client.userAgent.family)
                .lastLoginAt(Instant.now())
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
    public void deactivateDevice(HttpServletRequest request, int userId) {
        String userAgent = request.getHeader("User-Agent");
        Parser uaParser = new Parser();
        Client client = uaParser.parse(userAgent);
        String ipAdress = request.getRemoteAddr();
        String browserName = client.userAgent.family;
        userDeviceRepository.findByIpAddressAndBrowserNameAndUser_Uid(ipAdress, browserName, userId)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setLogoutAt(Instant.now()); // Thêm trường logoutAt nếu cần
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
}