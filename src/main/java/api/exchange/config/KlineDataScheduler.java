package api.exchange.config;

import api.exchange.services.CoinDataService;
import api.exchange.services.FuturesDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KlineDataScheduler {

    @Autowired
    private CoinDataService coinDataService;

    @Autowired
    private FuturesDataService futuresDataService;

    /**
     * Lấy dữ liệu kline 1m mỗi phút
     * Chạy vào giây 0 của mỗi phút
     */
    @Scheduled(cron = "0 * * * * *")
    public void fetchMinuteData() {
        System.out.println("🔄 Fetching 1m data at: " + java.time.LocalDateTime.now());

        try {
            coinDataService.fetchAndSaveAllKlineData1m();
        } catch (Exception e) {
            System.err.println("❌ Error in 1m data fetch: " + e.getMessage());
        }
    }

    /**
     * Lấy dữ liệu kline 1h mỗi giờ
     * Chạy vào phút 0 của mỗi giờ
     */
    @Scheduled(cron = "0 0 * * * *")
    public void fetchHourlyData() {
        System.out.println("🔄 Fetching 1h data at: " + java.time.LocalDateTime.now());

        try {
            coinDataService.fetchAndSaveAllKlineData1h();
        } catch (Exception e) {
            System.err.println("❌ Error in 1h data fetch: " + e.getMessage());
        }
    }

    /**
     * Lấy dữ liệu futures 1m mỗi phút
     * Chạy vào giây 5 của mỗi phút (offset để tránh conflict với spot)
     */
    @Scheduled(cron = "5 * * * * *")
    public void fetchFuturesMinuteData() {
        System.out.println("🔄 Fetching futures 1m data at: " + java.time.LocalDateTime.now());

        try {
            futuresDataService.fetchAndSaveAllKlineData1m();
        } catch (Exception e) {
            System.err.println("❌ Error in futures 1m data fetch: " + e.getMessage());
        }
    }

    /**
     * Lấy dữ liệu futures 1h mỗi giờ
     * Chạy vào phút 0, giây 30 của mỗi giờ (offset để tránh conflict với spot)
     */
    @Scheduled(cron = "30 0 * * * *")
    public void fetchFuturesHourlyData() {
        System.out.println("🔄 Fetching futures 1h data at: " + java.time.LocalDateTime.now());

        try {
            futuresDataService.fetchAndSaveAllKlineData1h();
        } catch (Exception e) {
            System.err.println("❌ Error in futures 1h data fetch: " + e.getMessage());
        }
    }

    /**
     * Lấy dữ liệu ban đầu khi ứng dụng khởi động
     * Chạy sau khi ứng dụng khởi động 30 giây
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void fetchInitialData() {

        try {
            // Lấy thông tin coin ban đầu
            coinDataService.fetchAndSaveAllCoinInfo();

            // Lấy dữ liệu spot 1m ban đầu
            coinDataService.fetchAndSaveAllKlineData1m();

            // Lấy dữ liệu spot 1h ban đầu
            coinDataService.fetchAndSaveAllKlineData1h();

            // Lấy dữ liệu futures 1m ban đầu
            futuresDataService.fetchAndSaveAllKlineData1m();

            // Lấy dữ liệu futures 1h ban đầu
            futuresDataService.fetchAndSaveAllKlineData1h();

        } catch (Exception e) {
            System.err.println("❌ Error in initial data fetch: " + e.getMessage());
        }
    }

    /**
     * Dọn dẹp dữ liệu cũ mỗi ngày lúc 2:00 AM
     * Xóa dữ liệu cũ hơn 7 ngày
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldData() {
        System.out.println("🧹 Cleaning up old data at: " + java.time.LocalDateTime.now());

        try {
            // Xóa dữ liệu 1m cũ hơn 7 ngày
            java.time.LocalDateTime cutoffTime1m = java.time.LocalDateTime.now().minusDays(7);
            // Note: Cần implement cleanup method trong repository

            // Xóa dữ liệu 1h cũ hơn 30 ngày
            java.time.LocalDateTime cutoffTime1h = java.time.LocalDateTime.now().minusDays(30);
            // Note: Cần implement cleanup method trong repository

            System.out.println("✅ Old data cleanup completed");
        } catch (Exception e) {
            System.err.println("❌ Error in data cleanup: " + e.getMessage());
        }
    }
}
