package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
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
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private TransactionSpotRepository transactionSpotRepository;

    @Autowired
    private SpotWalletService spotWalletService;

    @Transactional
    public void matchOrders(OrderBooks newOrder) {
        log.info("🔍 Starting matching for order ID: {}", newOrder.getId());

        if (newOrder.isFullyFilled()) {
            log.info("🟡 Order {} already fully filled", newOrder.getId());
            return;
        }

        // Fetch matching orders from DB directly
        List<OrderBooks> oppositeOrders;
        if (newOrder.isBuyOrder()) {
            // If buying, look for Sell orders (lowest ask first)
            oppositeOrders = orderBooksRepository.findSellOrderBooks(newOrder.getSymbol());
        } else {
            // If selling, look for Buy orders (highest bid first)
            oppositeOrders = orderBooksRepository.findBuyOrderBooks(newOrder.getSymbol());
        }

        if (oppositeOrders.isEmpty()) {
            log.info("📥 No opposite orders found in DB. Order ID: {}", newOrder.getId());
            return;
        }

        log.info("🔍 Matching order {} with {} opposite orders from DB", newOrder.getId(), oppositeOrders.size());

        Iterator<OrderBooks> iterator = oppositeOrders.iterator();
        while (iterator.hasNext() && !newOrder.isFullyFilled()) {
            OrderBooks oppositeOrder = iterator.next();

            if (!isValidOrder(oppositeOrder)) {
                log.warn("⚠️ Skipping invalid order from DB: {}", oppositeOrder.getId());
                continue;
            }

            if (canMatch(newOrder, oppositeOrder)) {
                // Double check status from DB in case of concurrency (optional but good
                // practice)
                // Since we just fetched it, it might be stale if high concurrency, but we are
                // inside transaction?
                // Note: @Transactional ensures we are in a transaction context.
                // Ideally, we should lock these rows, but for now simple matching.

                executeTrade(newOrder, oppositeOrder);
            }
        }

        handleOrderAfterMatching(newOrder);
    }

    private void executeTrade(OrderBooks newOrder, OrderBooks oppositeOrder) {
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

        // Cập nhật opposite order trong DB
        orderBooksRepository.save(oppositeOrder);

        // Tạo trade record
        createTradeRecord(newOrder, oppositeOrder, matchQuantity, tradePrice);

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
        } else {
            log.info("🟡 Order partially filled: {} (filled: {}/{})",
                    order.getId(), order.getFilledQuantity(), order.getQuantity());
        }

        // Luôn cập nhật vào DB ngay lập tức để đảm bảo tính nhất quán
        orderBooksRepository.save(order);
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

    private void createTradeRecord(OrderBooks newOrder, OrderBooks oppositeOrder,
            BigDecimal mathQuality, BigDecimal tradePrice) {
        try {
            TransactionSpot transactionSpot = new TransactionSpot();
            if (newOrder.isBuyOrder()) {
                transactionSpot.setBuyerId(newOrder.getUid());
                transactionSpot.setBuyerOrderId(newOrder.getId());
                transactionSpot.setSellerId(oppositeOrder.getUid());
                transactionSpot.setSellerOrderId(oppositeOrder.getId());
                transactionSpot.setSide(api.exchange.models.TransactionSpot.TradeSide.BUY);
            } else {
                transactionSpot.setSellerId(newOrder.getUid());
                transactionSpot.setSellerOrderId(newOrder.getId());
                transactionSpot.setBuyerOrderId(oppositeOrder.getId());
                transactionSpot.setBuyerId(oppositeOrder.getUid());
                transactionSpot.setSide(api.exchange.models.TransactionSpot.TradeSide.SELL);
            }
            transactionSpot.setSymbol(newOrder.getSymbol());
            transactionSpot.setQuantity(mathQuality);
            transactionSpot.setPrice(tradePrice);
            transactionSpotRepository.save(transactionSpot);
        } catch (Exception e) {
            log.error("Error creating trade record:", e);
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