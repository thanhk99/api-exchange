package api.exchange.models;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(name = "UK_user_refresh_token", columnNames = {
        "uid" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@AllArgsConstructor
@Builder
public class refreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "uid", referencedColumnName = "uid")
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;
}
