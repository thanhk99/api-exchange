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
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.models.SpotWalletHistory;
import api.exchange.models.TransactionSpot;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.TransactionSpotRepository;
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

    @Autowired
    private TransactionSpotRepository transactionSpotRepository;

    @Autowired
    private SpotWalletService spotWalletService;

    public void addOrderToRedis(OrderBooks order) {
        String key = order.isBuyOrder() ? "buyOrders:" + order.getSymbol() : "sellOrders:" + order.getSymbol();
        double score = order.isMarketOrder() ? 0 : order.getPrice().doubleValue() * (order.isBuyOrder() ? -1 : 1);
        redisTemplate.opsForZSet().add(key, order, score);
        log.info("➕ Added order to Redis: {} with score {}", order.getId(), score);
    }

    @Transactional
    public void matchOrders(OrderBooks newOrder) {
        log.info("🔍 Starting matching for order ID: {}", newOrder.getId());

        if (newOrder.isFullyFilled()) {
            log.info("🟡 Order {} already fully filled", newOrder.getId());
            return;
        }

        String oppositeKey = getOppositeOrderKey(newOrder);
        Set<OrderBooks> oppositeOrders = getOppositeOrdersFromRedis(oppositeKey);

        if (oppositeOrders.isEmpty()) {
            addOrderToRedis(newOrder);
            log.info("📥 No opposite orders, added to Redis. Order ID: {}", newOrder.getId());
            return;
        }

        log.info("🔍 Matching order {} with {} opposite orders", newOrder.getId(), oppositeOrders.size());

        // Sử dụng iterator để tránh concurrent modification
        Iterator<OrderBooks> iterator = oppositeOrders.iterator();
        while (iterator.hasNext() && !newOrder.isFullyFilled()) {
            OrderBooks oppositeOrder = iterator.next();

            if (!isValidOrder(oppositeOrder)) {
                log.warn("⚠️ Skipping invalid order from Redis: {}", oppositeOrder.getId());
                continue;
            }

            if (canMatch(newOrder, oppositeOrder)) {
                // THÊM: Lấy order mới nhất từ DB trước khi khớp
                OrderBooks freshOppositeOrder = orderBooksRepository.findById(oppositeOrder.getId())
                        .orElse(oppositeOrder);

                executeTrade(newOrder, freshOppositeOrder, oppositeKey);
            }
        }

        handleOrderAfterMatching(newOrder);
    }

    private void executeTrade(OrderBooks newOrder, OrderBooks oppositeOrder, String oppositeKey) {
        // Tính toán match quantity dựa trên remaining quantity thực tế
        BigDecimal newOrderRemaining = newOrder.getRemainingQuantity();
        BigDecimal oppositeRemaining = oppositeOrder.getRemainingQuantity();
        BigDecimal matchQuantity = newOrderRemaining.min(oppositeRemaining);

        BigDecimal tradePrice = calculateTradePrice(newOrder, oppositeOrder);

        log.info("🎯 Executing trade: {} units at {} between order {} (rem: {}) and order {} (rem: {})",
                matchQuantity, tradePrice,
                newOrder.getId(), newOrderRemaining,
                oppositeOrder.getId(), oppositeRemaining);

        // Cập nhật filled quantity - cộng dồn
        newOrder.setFilledQuantity(newOrder.getFilledQuantity().add(matchQuantity));
        oppositeOrder.setFilledQuantity(oppositeOrder.getFilledQuantity().add(matchQuantity));

        log.info("📊 After trade - Order {}: filled {}/{}, Order {}: filled {}/{}",
                newOrder.getId(), newOrder.getFilledQuantity(), newOrder.getQuantity(),
                oppositeOrder.getId(), oppositeOrder.getFilledQuantity(), oppositeOrder.getQuantity());

        // Cập nhật trạng thái
        updateOrderStatus(newOrder);
        updateOrderStatus(oppositeOrder);

        // Cập nhật opposite order trong Redis và DB
        updateOrderInRedisAndDB(oppositeOrder, oppositeKey);

        // Tạo trade record
        createTradeRecord(newOrder, oppositeOrder, oppositeKey, matchQuantity, tradePrice);

        // Execute Trade in Wallets (Transfer funds)
        try {
            // Determine buyer and seller
            String buyerUid = newOrder.isBuyOrder() ? newOrder.getUid() : oppositeOrder.getUid();
            String sellerUid = newOrder.isBuyOrder() ? oppositeOrder.getUid() : newOrder.getUid();

            // Determine TradeType for wallet logic
            api.exchange.models.SpotHistory.TradeType walletTradeType;
            if (newOrder.isMarketOrder() && oppositeOrder.isMarketOrder()) {
                walletTradeType = api.exchange.models.SpotHistory.TradeType.MARKET_MARKET;
            } else if (newOrder.isBuyOrder() && newOrder.isMarketOrder() && !oppositeOrder.isMarketOrder()) {
                walletTradeType = api.exchange.models.SpotHistory.TradeType.MARKET_LIMIT_BUY;
            } else if (!newOrder.isBuyOrder() && newOrder.isMarketOrder() && !oppositeOrder.isMarketOrder()) {
                walletTradeType = api.exchange.models.SpotHistory.TradeType.MARKET_LIMIT_SELL;
            } else if (!newOrder.isMarketOrder() && oppositeOrder.isMarketOrder()) {
                if (newOrder.isBuyOrder()) {
                    walletTradeType = api.exchange.models.SpotHistory.TradeType.MARKET_LIMIT_SELL;
                } else {
                    walletTradeType = api.exchange.models.SpotHistory.TradeType.MARKET_LIMIT_BUY;
                }
            } else {
                walletTradeType = api.exchange.models.SpotHistory.TradeType.LIMIT_LIMIT;
            }

            // Calculate locked price (for Limit orders)
            BigDecimal lockedPrice = BigDecimal.ZERO;
            if (newOrder.isBuyOrder() && !newOrder.isMarketOrder()) {
                lockedPrice = newOrder.getPrice();
            } else if (oppositeOrder.isBuyOrder() && !oppositeOrder.isMarketOrder()) {
                lockedPrice = oppositeOrder.getPrice();
            }

            spotWalletService.executeTradeSpot(sellerUid, buyerUid, tradePrice, matchQuantity, walletTradeType,
                    newOrder.getSymbol(), lockedPrice);

        } catch (Exception e) {
            log.error("❌ Failed to execute wallet trade: {}", e.getMessage());
        }

        log.info("✅ Trade executed successfully");
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

    private Set<OrderBooks> getOppositeOrdersFromRedis(String oppositeKey) {
        try {
            Set<OrderBooks> redisOrders = redisTemplate.opsForZSet().range(oppositeKey, 0, -1);
            if (redisOrders == null || redisOrders.isEmpty()) {
                return new HashSet<>();
            }

            // Tạo set mới với orders đã được refresh từ DB
            Set<OrderBooks> refreshedOrders = new HashSet<>();
            for (OrderBooks redisOrder : redisOrders) {
                try {
                    // Lấy order mới nhất từ DB để có filled quantity chính xác
                    OrderBooks freshOrder = orderBooksRepository.findById(redisOrder.getId())
                            .orElse(redisOrder);
                    refreshedOrders.add(freshOrder);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to refresh order {} from DB, using Redis version: {}",
                            redisOrder.getId(), e.getMessage());
                    refreshedOrders.add(redisOrder);
                }
            }

            return refreshedOrders;

        } catch (Exception e) {
            log.error("❌ Error reading from Redis: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    private boolean isValidOrder(OrderBooks order) {
        if (order == null) {
            log.warn("⚠️ Order is null");
            return false;
        }

        if (order.getId() == null) {
            log.warn("⚠️ Order has null ID: {}", order);
            return false;
        }

        if (order.getQuantity() == null || order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ Order has invalid quantity: {}", order.getId());
            return false;
        }

        if (order.isLimitOrder() && order.getPrice() == null) {
            log.warn("⚠️ Limit order has null price: {}", order.getId());
            return false;
        }

        return true;
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

    private void updateOrderInRedisAndDB(OrderBooks order, String redisKey) {
        try {
            // LUÔN cập nhật DB trước
            OrderBooks updatedOrder = orderBooksRepository.save(order);
            log.info("💾 Order saved to DB: {} (filled: {}/{}, status: {})",
                    updatedOrder.getId(), updatedOrder.getFilledQuantity(),
                    updatedOrder.getQuantity(), updatedOrder.getStatus());

            // Sau đó cập nhật Redis
            if (updatedOrder.isFullyFilled()) {
                // Xóa khỏi Redis nếu đã filled hết
                removeOrderFromRedis(redisKey, updatedOrder);
                log.info("🗑️ Order fully filled, removed from Redis: {}", updatedOrder.getId());
            } else {
                // Cập nhật order trong Redis với data mới nhất từ DB
                updateOrderInRedis(redisKey, updatedOrder);
                log.info("📝 Order updated in Redis: {} (filled: {}/{})",
                        updatedOrder.getId(), updatedOrder.getFilledQuantity(),
                        updatedOrder.getQuantity());
            }

        } catch (Exception e) {
            log.error("❌ Failed to update order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void updateOrderInRedis(String redisKey, OrderBooks order) {
        try {
            // Xóa order cũ khỏi Redis
            removeOrderFromRedis(redisKey, order);

            // Thêm order mới với filled quantity đã cập nhật
            Double score = calculateScore(order);
            redisTemplate.opsForZSet().add(redisKey, order, score);

            log.debug("🔄 Order updated in Redis: {} with score {}", order.getId(), score);

        } catch (Exception e) {
            log.error("❌ Failed to update order {} in Redis: {}", order.getId(), e.getMessage());
        }
    }

    private void removeOrderFromRedis(String redisKey, OrderBooks orderToRemove) {
        try {
            Set<OrderBooks> ordersInRedis = redisTemplate.opsForZSet().range(redisKey, 0, -1);
            if (ordersInRedis != null) {
                for (OrderBooks order : ordersInRedis) {
                    if (order.getId().equals(orderToRemove.getId())) {
                        redisTemplate.opsForZSet().remove(redisKey, order);
                        log.debug("🧹 Removed order {} from Redis", order.getId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to remove order {} from Redis: {}", orderToRemove.getId(), e.getMessage());
        }
    }

    private double calculateScore(OrderBooks order) {
        if (order.isMarketOrder()) {
            return 0;
        }

        double score = order.getPrice().doubleValue();
        return order.isBuyOrder() ? -score : score;
    }

    private void createTradeRecord(OrderBooks newOrder, OrderBooks oppositeOrder, String oppositeKey,
            BigDecimal mathQuality, BigDecimal tradePrice) {
        try {
            TransactionSpot transactionSpot = new TransactionSpot();
            if (newOrder.isBuyOrder()) {
                transactionSpot.setBuyerId(newOrder.getUid());
                transactionSpot.setBuyerOrderId(newOrder.getId());
                transactionSpot.setSellerId(oppositeOrder.getUid());
                transactionSpot.setSellerOrderId(oppositeOrder.getId());
            } else {
                transactionSpot.setSellerId(newOrder.getUid());
                transactionSpot.setSellerOrderId(newOrder.getId());
                transactionSpot.setBuyerOrderId(oppositeOrder.getId());
                transactionSpot.setBuyerId(oppositeOrder.getUid());
            }
            transactionSpot.setSymbol(newOrder.getSymbol());
            transactionSpot.setQuantity(mathQuality);
            transactionSpot.setPrice(tradePrice);
            transactionSpotRepository.save(transactionSpot);
        } catch (Exception e) {
            log.error("Error creating trade record for key {}:", oppositeKey, e);
        }
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