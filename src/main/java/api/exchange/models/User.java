package api.exchange.models;

import java.time.LocalDateTime;
import java.util.Random;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"user\"", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email")
})
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    private String uid;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "roles", nullable = false, length = 10)
    @Builder.Default
    private String roles = "USER";

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_level")
    @Builder.Default
    private UserLevel userLevel = UserLevel.BASIC;

    @Column(name = "password_level2", nullable = true, length = 6)
    private String passwordLevel2;

    @Column(name = "nation", nullable = false, length = 20)
    private String nation;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Version
    private Long version;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        if (this.uid == null) {
            this.uid = generateUID(18);
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.userStatus == null)
            this.userStatus = UserStatus.ACTIVE;
        if (this.kycStatus == null)
            this.kycStatus = KycStatus.NONE;
        if (this.userLevel == null)
            this.userLevel = UserLevel.BASIC;
        if (this.roles == null)
            this.roles = "USER";
    }

    public enum UserStatus {
        PENDING, SUSPENDED, ACTIVE, BANNED
    }

    public enum KycStatus {
        NONE, PENDING, VERIFIED, REJECTED
    }

    public enum UserLevel {
        BASIC, VERIFIED, VIP
    }

    // Phương thức để tạo UID ngẫu nhiên
    private String generateUID(int length) {
        Random random = new Random();
        StringBuilder uidBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int digit = random.nextInt(10);
            uidBuilder.append(digit);
        }

        return uidBuilder.toString();
    }
}
