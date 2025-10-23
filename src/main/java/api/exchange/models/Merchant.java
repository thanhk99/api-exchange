package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "uid", unique = true, nullable = false)
    private User user;

    @Column(name = "name_display")
    private String nameDisplay;

    @Column(name = "total_transactions")
    private Integer totalTransactions = 0;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate = BigDecimal.ZERO;

    // @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private List<P2PAd> ads;

}
