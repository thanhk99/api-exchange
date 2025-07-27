package api.exchange.models;

import java.time.LocalDateTime;
import java.util.Random;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Setter
@Getter
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String uid;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "roles", nullable = false, length = 10)
    private String roles;

    private boolean isVerified;
    private boolean isActive;

    @Column(name = "password_level2", nullable = true, length = 6)
    private String passwordLevel2;

    @Column(name = "nation", nullable = false, length = 20)
    private String nation;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    private LocalDateTime otp_verify;

    private Long login_fail_count;

    private String anti_frau_code;

    public User() {
        this.uid = generateUID(18);
    }

    // Phương thức để tạo UID ngẫu nhiên
    private String generateUID(int length) {
        Random random = new Random();
        StringBuilder uidBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int digit = random.nextInt(10); // Tạo số ngẫu nhiên từ 0 đến 9
            uidBuilder.append(digit);
        }

        return uidBuilder.toString();
    }
}
