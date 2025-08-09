package api.exchange.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_p2p_details")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class P2POrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private P2PAd ad;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "asset", nullable = false)
    private String asset; // USDT, BTC...

    @Column(name = "fiat_currency", nullable = false)
    private String fiatCurrency; // VND, USD...

    @Column(precision = 20, scale = 2)
    private BigDecimal fiatAmount; // Số tiền fiat

    @Column(precision = 20, scale = 8)
    private BigDecimal cryptoAmount; // Số lượng coin

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private P2POrderDetail.P2PTransactionStatus status;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Thời gian hết hạn để thanh toán

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public enum P2PTransactionStatus {
        ORDER_PLACED, // Người mua tạo lệnh
        AWAITING_PAYMENT, // Đã xác nhận, chờ người mua thanh toán
        PAYMENT_SENT, // Người mua đã xác nhận chuyển tiền
        AWAITING_RELEASE, // Người bán chờ giải phóng coin
        COMPLETED, // Giao dịch hoàn tất
        CANCELLED, // Giao dịch bị hủy
        DISPUTE_OPENED // Tranh chấp được mở
    }
}
