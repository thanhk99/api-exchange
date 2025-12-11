package api.exchange.repository;

import api.exchange.models.P2POrderDetail;
import api.exchange.models.P2POrderDetail.P2PTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface P2POrderDetailRepository extends JpaRepository<P2POrderDetail, Long> {
    // Tìm giao dịch theo ID và người mua
    P2POrderDetail findByIdAndBuyerId(Long id, String buyerId);

    // Tìm giao dịch theo ID và người bán
    P2POrderDetail findByIdAndSellerId(Long id, String sellerId);

    // Đếm tổng số giao dịch của user (cả mua và bán)
    @Query("SELECT COUNT(o) FROM P2POrderDetail o WHERE o.buyerId = :userId OR o.sellerId = :userId")
    Long countTotalTransactions(@Param("userId") String userId);

    // Đếm số giao dịch hoàn thành
    @Query("SELECT COUNT(o) FROM P2POrderDetail o WHERE (o.buyerId = :userId OR o.sellerId = :userId) AND o.status = :status")
    Long countTransactionsByStatus(@Param("userId") String userId, @Param("status") P2PTransactionStatus status);

    // Lấy lịch sử giao dịch của user
    @Query("SELECT o FROM P2POrderDetail o WHERE o.buyerId = :userId OR o.sellerId = :userId ORDER BY o.createdAt DESC")
    List<P2POrderDetail> findUserTransactionHistory(@Param("userId") String userId);

}
