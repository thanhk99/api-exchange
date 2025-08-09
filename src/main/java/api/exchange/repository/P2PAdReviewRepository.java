package api.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import api.exchange.models.P2PAdReview;
import java.util.List;

public interface P2PAdReviewRepository extends JpaRepository<P2PAdReview, Long> {
    // Tìm đánh giá theo quảng cáo
    List<P2PAdReview> findByAdId(Long adId);

    // Tìm đánh giá theo người được đánh giá
    List<P2PAdReview> findByUserId(Long UserId);

    // Tính điểm trung bình
    // @Query("SELECT AVG(r.rating) FROM P2PAdReview r WHERE r.userId = :userId")
    // Double calculateAverageRating(@Param("userId") Long userId);

    // @Query("slect ")
}