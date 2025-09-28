package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.SpotWalletHistory;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.repository.SpotWalletHistoryRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderBooksService {


    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    private RedisTemplate<String, OrderBooks> redisTemplate;

    @Autowired
    private OrderBatchProcessor batchProcessor;

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    public void addOrderToRedis(OrderBooks order) {
        String key  = order.isBuyOrder() ? "buyOrders:" + order.getSymbol() : "sellOrders:" + order.getSymbol();
        double score = order.isMarketOrder() ? 0 : 
        order.getPrice().doubleValue() * (order.isBuyOrder() ? -1 : 1);
        redisTemplate.opsForZSet().add(key, order, score);
        log.info("➕ Added order to Redis: {} with score {}", order.getId(), score);
    }

    @Transactional
    public void matchOrders(OrderBooks newOrder) {
        if (newOrder.isFullyFilled()) {
            log.info("🟡 Order {} already fully filled", newOrder.getId());
            return;
        }
        
        String oppositeKey = getOppositeOrderKey(newOrder);
        Set<OrderBooks> oppositeOrders = redisTemplate.opsForZSet().range(oppositeKey, 0, -1);
        
        if (oppositeOrders == null || oppositeOrders.isEmpty()) {
            // Không có orders đối lập, thêm vào Redis
            addOrderToRedis(newOrder);
            log.info("📥 No opposite orders, added to Redis: {}", newOrder.getId());
            return;
        }
        
        log.info("🔍 Matching order {} with {} opposite orders", newOrder.getId(), oppositeOrders.size());
        
        // Duyệt qua các orders đối lập để khớp
        for (OrderBooks oppositeOrder : oppositeOrders) {
            if (newOrder.isFullyFilled()) break;
            if (!validateOrder(oppositeOrder)) continue;
            
            if (canMatch(newOrder, oppositeOrder)) {
                executeTrade(newOrder, oppositeOrder, oppositeKey);
            }
        }
        
        // Xử lý newOrder sau khi matching
        handleOrderAfterMatching(newOrder);
    }
    private void executeTrade(OrderBooks newOrder, OrderBooks oppositeOrder, String oppositeKey) {
        BigDecimal matchQuantity = newOrder.getRemainingQuantity()
            .min(oppositeOrder.getRemainingQuantity());
        BigDecimal tradePrice = calculateTradePrice(newOrder, oppositeOrder);
        
        log.info("🎯 Executing trade: {} units at {} between order {} and {}", 
                matchQuantity, tradePrice, newOrder.getId(), oppositeOrder.getId());
        
        // Cập nhật filled quantity
        newOrder.setFilledQuantity(newOrder.getFilledQuantity().add(matchQuantity));
        oppositeOrder.setFilledQuantity(oppositeOrder.getFilledQuantity().add(matchQuantity));
        
        // Cập nhật trạng thái
        updateOrderStatus(newOrder);
        updateOrderStatus(oppositeOrder);
        
        // Cập nhật opposite order trong Redis
        updateOrderInRedis(oppositeKey, oppositeOrder);
        
        // Tạo trade record
        createTradeRecord(newOrder, oppositeOrder, matchQuantity, tradePrice);
        
        log.info("✅ Trade executed: {} -> {} units filled", 
                newOrder.getId(), newOrder.getFilledQuantity());
    }
    

    private void handleOrderAfterMatching(OrderBooks order) {
        updateOrderStatus(order);
        
        if (order.isFullyFilled()) {
            log.info("✅ Order fully filled: {}", order.getId());
            // Không cần thêm vào Redis vì đã filled
        } else {
            // Order chưa filled hết, thêm vào Redis để chờ khớp tiếp
            addOrderToRedis(order);
            log.info("🟡 Order partially filled, added to Redis: {} (filled: {}/{})", 
                    order.getId(), order.getFilledQuantity(), order.getQuantity());
        }
        
        // Luôn cập nhật vào DB
        batchProcessor.addOrderToBatch(order);
    }

    private void updateOrderStatus(OrderBooks order) {
        if (order.isFullyFilled()) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            order.setStatus(OrderStatus.ACTIVE);
        }
        order.setUpdatedAt(LocalDateTime.now());
    }
    
    private String getOppositeOrderKey(OrderBooks order) {
        return order.isBuyOrder() ? "sellOrders:" + order.getSymbol() : "buyOrders:" + order.getSymbol();
    }

    private boolean validateOrder(OrderBooks order) {
        return order != null && 
               order.getQuantity() != null &&
               order.getQuantity().compareTo(BigDecimal.ZERO) > 0 &&
               (order.isMarketOrder() || order.getPrice() != null);
    }

    private boolean canMatch(OrderBooks newOrder, OrderBooks oppositeOrder) {
        // Market orders always match
        if (newOrder.isMarketOrder() || oppositeOrder.isMarketOrder()) {
            return true;
        }

        // Limit order matching logic
        if (newOrder.isBuyOrder()) {
            return newOrder.getPrice().compareTo(oppositeOrder.getPrice()) >= 0;
        } else {
            return newOrder.getPrice().compareTo(oppositeOrder.getPrice()) <= 0;
        }
    }

    private BigDecimal calculateTradePrice(OrderBooks order1, OrderBooks order2) {
        // Priority: existing limit order price > market order logic
        if (order1.isMarketOrder() && !order2.isMarketOrder()) {
            return order2.getPrice();
        } else if (!order1.isMarketOrder() && order2.isMarketOrder()) {
            return order1.getPrice();
        } else if (order1.isMarketOrder() && order2.isMarketOrder()) {
            return getLastTradedPrice(order1.getSymbol());
        } else {
            // Both are limit orders - use the opposite order's price
            return order1.isBuyOrder() ? order2.getPrice() : order1.getPrice();
        }
    }

    private void updateOrderInRedis(String key, OrderBooks order) {
        if (order.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            redisTemplate.opsForZSet().remove(key, order);
            log.debug("🗑️ Order removed from Redis: {}", order.getId());
        } else {
            redisTemplate.opsForZSet().add(key, order, calculateScore(order));
            log.debug("📝 Order updated in Redis: {}", order.getId());
        }
    }

    private double calculateScore(OrderBooks order) {
        if (order.isMarketOrder()) {
            return 0;
        }

        double score = order.getPrice().doubleValue();
        return order.isBuyOrder() ? -score : score;
    }
    private void createTradeRecord(OrderBooks buyOrder, OrderBooks sellOrder, 
                                   BigDecimal quantity, BigDecimal price) {
        // Tạo và lưu trade record vào DB (chưa implement chi tiết)
        SpotWalletHistory spotWalletHistory = new SpotWalletHistory();  
        spotWalletHistory.setAsset(buyOrder.getSymbol());
        spotWalletHistory.setNote("Trade executed between orders"+ buyOrder.getId() + " and " + sellOrder.getId());
        spotWalletHistory.setType("TRADE");
        spotWalletHistory.setBalance(quantity.multiply(price));
        spotWalletHistory.setCreateDt(LocalDateTime.now());
        spotWalletHistoryRepository.save(spotWalletHistory);
        log.info("📝 Trade record created: BuyOrder {}, SellOrder {}, Quantity {}, Price {}", 
                buyOrder.getId(), sellOrder.getId(), quantity, price);
    }
    public BigDecimal getLastTradedPrice(String symbol) {
        // Trong thực tế, lấy từ database hoặc cache
        // Tạm thời return giá mặc định
        return BigDecimal.valueOf(100); // Example price
    }

    public List<OrderBooks> listOrderBookBuy(String symbol) {
        List<OrderBooks> listOrderBooks = orderBooksRepository.findActiveOrders(symbol);
        return listOrderBooks;
    }

}