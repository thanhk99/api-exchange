package api.exchange.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "earn_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarnProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(precision = 5, scale = 2)
    private BigDecimal apy = BigDecimal.ZERO;

    @Column(precision = 24, scale = 8)
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "create_dt")
    private BigDecimal createDt;

    @Column(name = "update_dt")
    private BigDecimal updateDt;

    @Column(name = "duration_days")
    private int durationDays;

    private boolean isActive = true;
}