package api.exchange.models;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "uid")
    private User user;

    @Column(nullable = false)
    private String deviceId; // UUID tạo phía client

    private String deviceName; // Tên thiết bị (tùy chọn)
    private String deviceType; // MOBILE/WEB/DESKTOP
    private String ipAddress;
    private String location; // Tùy chọn

    @Column(nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(nullable = true)
    private LocalDateTime LogoutAt;

    private String browserName;

    @Column(nullable = false)
    private boolean isActive;

    @Column(unique = true, length = 1000)
    private String token;

}
