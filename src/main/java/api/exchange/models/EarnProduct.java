package api.exchange.models;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "earn_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarnProduct {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID productId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(precision = 5, scale = 2)
    private BigDecimal apy = BigDecimal.ZERO;

    private Integer durationDays;

    @Column(precision = 24, scale = 8)
    private BigDecimal minAmount = BigDecimal.ZERO;

    private boolean isActive = true;
}