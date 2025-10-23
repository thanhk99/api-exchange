package api.exchange.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import api.exchange.models.OrderBooks;
import api.exchange.repository.OrderBooksRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderBatchProcessor  {
        private final OrderBooksRepository orderBooksRepository;
        
        // Batch configuration
        private static final int BATCH_SIZE = 100;
        private static final long FLUSH_INTERVAL = 1000; // 1 second
        private static final String ORDER_QUEUE_KEY = "batch:orders";

        @Autowired
        private RedisTemplate<String, OrderBooks> redisTemplate;        

        private final List<OrderBooks> orderBatch = new ArrayList<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        public OrderBatchProcessor(OrderBooksRepository orderBooksRepository) {
            this.orderBooksRepository = orderBooksRepository;
            
            // Schedule batch flushing
            scheduler.scheduleAtFixedRate(this::flushBatches, FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MILLISECONDS);
        }
        
        // Add order to batch queue
        public void addOrderToBatch(OrderBooks order) {
            synchronized (orderBatch) {
                orderBatch.add(order);
                if (orderBatch.size() >= BATCH_SIZE) {
                    flushOrderBatch();
                }
            }
            
            // Also add to Redis for persistence
            redisTemplate.opsForList().rightPush(ORDER_QUEUE_KEY, order);
        }

        private void flushOrderBatch() {
            List<OrderBooks> batchToSave;
            synchronized (orderBatch) {
                if (orderBatch.isEmpty()) return;
                batchToSave = new ArrayList<>(orderBatch);
                orderBatch.clear();
            }
            
            if (!batchToSave.isEmpty()) {
                try {
                    orderBooksRepository.saveAll(batchToSave);
                    log.info("✅ Saved {} orders to database via batch", batchToSave.size());
                } catch (Exception e) {
                    log.error("❌ Failed to save order batch, re-queueing", e);
                    // Re-queue failed batch
                    batchToSave.forEach(this::addOrderToBatch);
                }
            }
        }
        
        // Scheduled batch flushing
        private void flushBatches() {
            flushOrderBatch();
        }
        
        // Recovery mechanism for system crash
        @PostConstruct
        public void recoverFromRedis() {
            log.info("🔄 Recovering batches from Redis...");
            recoverOrdersFromRedis();
        }
        
        private void recoverOrdersFromRedis() {
            try {
                List<OrderBooks> orders = redisTemplate.opsForList().range(ORDER_QUEUE_KEY, 0, -1);
                if (orders != null) {
                    orders.forEach(order -> addOrderToBatch((OrderBooks) order));
                    redisTemplate.delete(ORDER_QUEUE_KEY);
                    log.info("🔄 Recovered {} orders from Redis", orders.size());
                }
            } catch (Exception e) {
                log.error("❌ Failed to recover orders from Redis", e);
            }
        }
}
