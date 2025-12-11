package api.exchange.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tron_wallet")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TronWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uid; // Link to User.uid

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String encryptedPrivateKey;

    @Column(nullable = false)
    private String hexAddress;
}
