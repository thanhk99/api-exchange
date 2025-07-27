package api.exchange.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "p2p_ad_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class P2PAdReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(name = "ad_id", nullable = false)
    private Long adId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
