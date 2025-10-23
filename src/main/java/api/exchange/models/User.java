package api.exchange.models;

import java.time.LocalDateTime;
import java.util.Random;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"user\"")
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

    @Column(name = "phone")
    private String phone ;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "roles", nullable = false, length = 10)
    private String roles;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_level")
    private UserLevel userLevel;

    @Column(name = "password_level2", nullable = true, length = 6)
    private String passwordLevel2;

    @Column(name = "nation", nullable = false, length = 20)
    private String nation;

    @Column(name = "create_at" , nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at" )
    private LocalDateTime updateAt; 

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public User() {
        this.uid = generateUID(18);
        this.kycStatus = KycStatus.NONE;
        this.userLevel = UserLevel.BASIC;
        this.userStatus = UserStatus.ACTIVE;
        this.roles = "USER";
    }

    public enum UserStatus{
        PENDING , SUSPENDED , ACTIVE , BANNED 
    }

    public enum KycStatus{
        NONE , PENDING , VERIFIED , REJECTED 
    }

    public enum UserLevel{
        BASIC , VERIFIED , VIP 
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
