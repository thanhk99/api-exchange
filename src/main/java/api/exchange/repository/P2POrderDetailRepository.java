package api.exchange.repository;
import api.exchange.models.P2POrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface P2POrderDetailRepository extends JpaRepository<P2POrderDetail, Long> {
    // Tìm giao dịch theo ID và người mua
    P2POrderDetail findByIdAndBuyerId(Long id, String buyerId);

    // Tìm giao dịch theo ID và người bán
    P2POrderDetail findByIdAndSellerId(Long id, String sellerId);

}

