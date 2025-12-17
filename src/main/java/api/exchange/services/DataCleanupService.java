package api.exchange.services;

import api.exchange.repository.FuturesKlineData1sRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataCleanupService {

    @Autowired
    private FuturesKlineData1sRepository futuresKlineData1sRepository;

    /**
     * Chạy mỗi ngày lúc 00:00 để xóa dữ liệu 1s cũ
     * Giữ lại 3 ngày dữ liệu
     * package api.exchange.services;
     * 
     * import api.exchange.repository.FuturesKlineData1sRepository;
     * import org.springframework.beans.factory.annotation.Autowired;
     * import org.springframework.scheduling.annotation.Scheduled;
     * import org.springframework.stereotype.Service;
     * import org.springframework.transaction.annotation.Transactional;
     * 
     * import java.time.LocalDateTime;
     * 
     * @Service
     *          public class DataCleanupService {
     * 
     * @Autowired
     *            private FuturesKlineData1sRepository futuresKlineData1sRepository;
     * 
     *            /**
     *            Chạy mỗi ngày lúc 00:00 để xóa dữ liệu 1s cũ
     *            Giữ lại 3 ngày dữ liệu
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupOldFutures1sData() {
        try {
            // Xóa dữ liệu cũ hơn 3 ngày
            // LocalDateTime cutoffTime = LocalDateTime.now().minusDays(3);
            // System.out.println("🧹 Starting cleanup of Futures 1s data older than: " +
            // cutoffTime);

            // futuresKlineData1sRepository.deleteByStartTimeBefore(cutoffTime);

            System.out.println("✅ Cleanup skipped: User requested to keep all data.");
        } catch (Exception e) {
            System.err.println("❌ Error during data cleanup: " + e.getMessage());
        }
    }
}
