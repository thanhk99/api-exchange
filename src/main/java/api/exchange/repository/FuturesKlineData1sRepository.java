package api.exchange.repository;

import api.exchange.models.FuturesKlineData1s;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FuturesKlineData1sRepository extends JpaRepository<FuturesKlineData1s, Long> {

    /**
     * Lấy N kline gần nhất của một symbol
     */
    @Query("SELECT k FROM FuturesKlineData1s k WHERE k.symbol = :symbol ORDER BY k.startTime DESC LIMIT :limit")
    List<FuturesKlineData1s> findLatestKlines(@Param("symbol") String symbol, @Param("limit") int limit);

    /**
     * Xóa dữ liệu cũ hơn cutoff time
     */
    void deleteByStartTimeBefore(LocalDateTime cutoffTime);
}
