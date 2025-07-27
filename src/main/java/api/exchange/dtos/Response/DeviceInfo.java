package api.exchange.dtos.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceInfo {
    private String deviceId; // UUID từ client
    private String userAgent;
    private String deviceName; // iPhone 14, Macbook Pro...
    private String deviceType; // MOBILE/WEB/DESKTOP
    private String deviceModel;
    private String os; // iOS/Android/Windows...
    private String browser; // Chrome/Safari...
    private String country;
    private String ipAddress;
}