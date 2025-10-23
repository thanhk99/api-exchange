package api.exchange.repository;

import api.exchange.models.SpotKlineData1h;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpotKlineData1hRepository extends JpaRepository<SpotKlineData1h, Long> {

        /**
         * Lấy 72 nến 1h gần nhất của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "ORDER BY s.startTime DESC LIMIT 72")
        List<SpotKlineData1h> findLatest72Klines(@Param("symbol") String symbol);

        /**
         * Lấy nến 1h gần nhất của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "ORDER BY s.startTime DESC LIMIT 1")
        SpotKlineData1h findLatestKline(@Param("symbol") String symbol);

        /**
         * Lấy nến 1h trong khoảng thời gian cụ thể
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime <= :endTime ORDER BY s.startTime ASC")
        List<SpotKlineData1h> findKlinesInTimeRange(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Lấy nến 1h chưa đóng của một symbol
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "AND s.isClosed = false ORDER BY s.startTime DESC")
        List<SpotKlineData1h> findOpenKlines(@Param("symbol") String symbol);

        /**
         * Xóa dữ liệu cũ hơn một thời điểm cụ thể
         */
        void deleteByStartTimeBefore(LocalDateTime cutoffTime);

        /**
         * Đếm số lượng nến 1h của một symbol
         */
        long countBySymbol(String symbol);

        /**
         * Lấy danh sách symbols có dữ liệu 1h
         */
        @Query("SELECT DISTINCT s.symbol FROM SpotKlineData1h s")
        List<String> findDistinctSymbols();

        /**
         * Lấy nến 1h để tính toán khoảng 6h (lấy 6 nến liên tiếp)
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime < :endTime " +
                        "ORDER BY s.startTime ASC")
        List<SpotKlineData1h> findKlinesFor6hCalculation(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Lấy nến 1h để tính toán khoảng 12h (lấy 12 nến liên tiếp)
         */
        @Query("SELECT s FROM SpotKlineData1h s WHERE s.symbol = :symbol " +
                        "AND s.startTime >= :startTime AND s.startTime < :endTime " +
                        "ORDER BY s.startTime ASC")
        List<SpotKlineData1h> findKlinesFor12hCalculation(@Param("symbol") String symbol,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        SpotKlineData1h findBySymbolAndStartTime(String symbol, LocalDateTime startTime);
}
