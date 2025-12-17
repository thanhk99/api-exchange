package api.exchange.repository;

import api.exchange.models.FuturesKlineData1m;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FuturesKlineData1mRepository extends JpaRepository<FuturesKlineData1m, Long> {

    /**
     * Tìm kline theo symbol và start time
     */
    FuturesKlineData1m findBySymbolAndStartTime(String symbol, LocalDateTime startTime);

    /**
     * Lấy N kline gần nhất của một symbol (số lượng tuỳ chỉnh theo limit)
     */
    @Query("SELECT k FROM FuturesKlineData1m k WHERE k.symbol = :symbol ORDER BY k.startTime DESC LIMIT :limit")
    List<FuturesKlineData1m> findLatestKlines(@Param("symbol") String symbol, @Param("limit") int limit);

    /**
     * Lấy 72 kline gần nhất của một symbol (backward compatibility)
     */
    @Query("SELECT k FROM FuturesKlineData1m k WHERE k.symbol = :symbol ORDER BY k.startTime DESC LIMIT 72")
    List<FuturesKlineData1m> findLatest72Klines(@Param("symbol") String symbol);

    /**
     * Xóa dữ liệu cũ hơn cutoff time
     */
    void deleteByStartTimeBefore(LocalDateTime cutoffTime);

    /**
     * Lấy N kline gần nhất trước một thời điểm cụ thể
     */
    @Query("SELECT k FROM FuturesKlineData1m k WHERE k.symbol = :symbol AND k.startTime < :endTime ORDER BY k.startTime DESC LIMIT :limit")
    List<FuturesKlineData1m> findBySymbolAndStartTimeBeforeOrderByStartTimeDesc(@Param("symbol") String symbol,
            @Param("endTime") LocalDateTime endTime, @Param("limit") int limit);
}
