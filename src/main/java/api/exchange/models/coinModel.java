package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coins")
@Setter
@Getter
@NoArgsConstructor
public class coinModel {
    @Id
    @Column(name = "id", length = 10)
    private String id; // "BTC", "ETH"

    @Column(nullable = false)
    private String name;

    @Column(name = "current_price", precision = 20, scale = 6)
    private BigDecimal currentPrice;

    @Column(name = "price_change_24h", precision = 10, scale = 2)
    private BigDecimal priceChange24h;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "logo_url")
    private String logoUrl;
}
