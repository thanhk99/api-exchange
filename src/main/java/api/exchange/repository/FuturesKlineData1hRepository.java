package api.exchange.repository;

import api.exchange.models.FuturesKlineData1h;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FuturesKlineData1hRepository extends JpaRepository<FuturesKlineData1h, Long> {

    /**
     * Tìm kline theo symbol và start time
     */
    FuturesKlineData1h findBySymbolAndStartTime(String symbol, LocalDateTime startTime);

    /**
     * Lấy N kline gần nhất của một symbol (số lượng tuỳ chỉnh theo limit)
     */
    @Query("SELECT k FROM FuturesKlineData1h k WHERE k.symbol = :symbol ORDER BY k.startTime DESC LIMIT :limit")
    List<FuturesKlineData1h> findLatestKlines(@Param("symbol") String symbol, @Param("limit") int limit);

    /**
     * Lấy 72 kline gần nhất của một symbol (backward compatibility)
     */
    @Query("SELECT k FROM FuturesKlineData1h k WHERE k.symbol = :symbol ORDER BY k.startTime DESC LIMIT 72")
    List<FuturesKlineData1h> findLatest72Klines(@Param("symbol") String symbol);

    /**
     * Xóa dữ liệu cũ hơn cutoff time
     */
    void deleteByStartTimeBefore(LocalDateTime cutoffTime);
}
