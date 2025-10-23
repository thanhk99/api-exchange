package api.exchange.repository;

import api.exchange.models.SpotKlineData1m;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpotKlineData1mRepository extends JpaRepository<SpotKlineData1m, Long> {

        /**
         * Lấy 72 nến 1m gần nhất của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "ORDER BY s.startTime DESC LIMIT 72")
        List<SpotKlineData1m> findLatest72Klines(@Param("symbol") String symbol);

        /**
         * Lấy nến 1m gần nhất của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "ORDER BY s.startTime DESC LIMIT 1")
        SpotKlineData1m findLatestKline(@Param("symbol") String symbol);

        /**
         * Lấy nến 1m trong khoảng thời gian cụ thể
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime <= :endTime ORDER BY s.startTime ASC")
        List<SpotKlineData1m> findKlinesInTimeRange(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Lấy nến 1m chưa đóng của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "AND s.isClosed = false ORDER BY s.startTime DESC")
        List<SpotKlineData1m> findOpenKlines(@Param("symbol") String symbol);

        /**
         * Xóa dữ liệu cũ hơn một thời điểm cụ thể
         */
        void deleteByStartTimeBefore(LocalDateTime cutoffTime);

        /**
         * Đếm số lượng nến 1m của một symbol
         */
        long countBySymbol(String symbol);

        /**
         * Lấy danh sách symbols có dữ liệu 1m
         */
        @Query("SELECT DISTINCT s.symbol FROM SpotKlineData1m s")
        List<String> findDistinctSymbols();

        /**
         * Lấy nến 1m để tính toán khoảng 5m (lấy 5 nến liên tiếp)
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime < :endTime " +
                        "ORDER BY s.startTime ASC")
        List<SpotKlineData1m> findKlinesFor5mCalculation(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Lấy nến 1m để tính toán khoảng 15m (lấy 15 nến liên tiếp)
         */
        @Query("SELECT s FROM SpotKlineData1m s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime < :endTime " +
                        "ORDER BY s.startTime ASC")
        List<SpotKlineData1m> findKlinesFor15mCalculation(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        SpotKlineData1m findBySymbolAndStartTime(String symbol, LocalDateTime startTime);
}
