package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.SpotHistory.TradeType;
import api.exchange.repository.OrderBooksRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderBooksService {

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    SpotHistoryService spotHistoryService;

    @Transactional
    public void matchOrders(String symbol) {
        log.debug("🔍 Starting matching for symbol: {}", symbol);

        // Lấy tất cả orders active
        List<OrderBooks> allOrders = orderBooksRepository.findActiveOrders(symbol);

        // Tách thành buy và sell orders
        List<OrderBooks> buyOrders = allOrders.stream()
                .filter(OrderBooks::isBuyOrder)
                .collect(Collectors.toList());

        List<OrderBooks> sellOrders = allOrders.stream()
                .filter(OrderBooks::isSellOrder)
                .collect(Collectors.toList());

        log.debug("📊 Buy orders: {}, Sell orders: {}", buyOrders.size(), sellOrders.size());

        // Xử lý market orders trước (ưu tiên cao nhất)
        processMarketOrders(buyOrders, sellOrders, symbol);

        // Sau đó xử lý limit orders
        processLimitOrders(buyOrders, sellOrders, symbol);
    }

    private void processMarketOrders(List<OrderBooks> buyOrders, List<OrderBooks> sellOrders, String symbol) {
        // Tách market orders
        List<OrderBooks> marketBuyOrders = buyOrders.stream()
                .filter(OrderBooks::isMarketOrder)
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());

        List<OrderBooks> marketSellOrders = sellOrders.stream()
                .filter(OrderBooks::isMarketOrder)
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());

        log.debug("🔄 Processing {} market buy and {} market sell orders",
                marketBuyOrders.size(), marketSellOrders.size());

        // Đầu tiên: Matching giữa market buy và market sell
        matchMarketWithMarket(marketBuyOrders, marketSellOrders);

        // Sau đó: Xử lý market buy orders với limit sell orders
        for (OrderBooks marketBuy : marketBuyOrders) {
            if (marketBuy.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            // Tìm limit sell orders có sẵn (sắp xếp giá thấp nhất trước)
            List<OrderBooks> availableSellOrders = sellOrders.stream()
                    .filter(OrderBooks::isLimitOrder)
                    .filter(order -> order.getStatus() == OrderStatus.PENDING)
                    .sorted(Comparator.comparing(OrderBooks::getPrice)
                            .thenComparing(OrderBooks::getCreatedAt))
                    .collect(Collectors.toList());

            matchMarketBuyWithLimitSells(marketBuy, availableSellOrders);
        }

        // Cuối cùng: Xử lý market sell orders với limit buy orders
        for (OrderBooks marketSell : marketSellOrders) {
            if (marketSell.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            // Tìm limit buy orders có sẵn (sắp xếp giá cao nhất trước)
            List<OrderBooks> availableBuyOrders = buyOrders.stream()
                    .filter(OrderBooks::isLimitOrder)
                    .filter(order -> order.getStatus() == OrderStatus.PENDING)
                    .sorted(Comparator.comparing(OrderBooks::getPrice).reversed()
                            .thenComparing(OrderBooks::getCreatedAt))
                    .collect(Collectors.toList());

            matchMarketSellWithLimitBuys(marketSell, availableBuyOrders);
        }
    }

    private void matchMarketWithMarket(List<OrderBooks> marketBuys, List<OrderBooks> marketSells) {
        // Sắp xếp theo thời gian (cũ nhất trước)
        marketBuys.sort(Comparator.comparing(OrderBooks::getCreatedAt));
        marketSells.sort(Comparator.comparing(OrderBooks::getCreatedAt));

        int buyIndex = 0;
        int sellIndex = 0;

        while (buyIndex < marketBuys.size() && sellIndex < marketSells.size()) {
            OrderBooks marketBuy = marketBuys.get(buyIndex);
            OrderBooks marketSell = marketSells.get(sellIndex);

            if (marketBuy.getStatus() != OrderStatus.PENDING ||
                    marketSell.getStatus() != OrderStatus.PENDING) {
                if (marketBuy.getStatus() != OrderStatus.PENDING)
                    buyIndex++;
                if (marketSell.getStatus() != OrderStatus.PENDING)
                    sellIndex++;
                continue;
            }

            BigDecimal matchQuantity = marketBuy.getQuantity().min(marketSell.getQuantity());

            // Với market vs market, cần lấy giá từ thị trường (last traded price)
            // Tạm thời dùng giá mặc định, trong thực tế nên lấy từ database
            BigDecimal marketPrice = getLastTradedPrice(marketBuy.getSymbol());

            if (marketPrice == null) {
                log.warn("⚠️ No market price available for symbol: {}", marketBuy.getSymbol());
                break;
            }

            log.info("🎯 MARKET-MARKET Match: {} @ {} between Buy#{} and Sell#{}",
                    matchQuantity, marketPrice, marketBuy.getId(), marketSell.getId());

            executeTrade(marketBuy, marketSell, marketPrice, matchQuantity, TradeType.MARKET_MARKET);

            if (marketBuy.getStatus() == OrderStatus.DONE) {
                buyIndex++;
            }
            if (marketSell.getStatus() == OrderStatus.DONE) {
                sellIndex++;
            }
        }
    }

    private void matchMarketBuyWithLimitSells(OrderBooks marketBuy, List<OrderBooks> limitSells) {
        BigDecimal remainingQuantity = marketBuy.getQuantity();

        for (OrderBooks limitSell : limitSells) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (limitSell.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            BigDecimal matchQuantity = remainingQuantity.min(limitSell.getQuantity());
            BigDecimal tradePrice = limitSell.getPrice(); // Market buy lấy giá của limit sell

            log.info("💰 MARKET BUY - LIMIT SELL: {} @ {} - Buy#{} vs Sell#{}",
                    matchQuantity, tradePrice, marketBuy.getId(), limitSell.getId());

            executeTrade(marketBuy, limitSell, tradePrice, matchQuantity, TradeType.MARKET_LIMIT_BUY);
            remainingQuantity = remainingQuantity.subtract(matchQuantity);
        }

        updateOrderQuantity(marketBuy, remainingQuantity);
    }

    private void matchMarketSellWithLimitBuys(OrderBooks marketSell, List<OrderBooks> limitBuys) {
        BigDecimal remainingQuantity = marketSell.getQuantity();

        for (OrderBooks limitBuy : limitBuys) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (limitBuy.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            BigDecimal matchQuantity = remainingQuantity.min(limitBuy.getQuantity());
            BigDecimal tradePrice = limitBuy.getPrice(); // Market sell lấy giá của limit buy

            log.info("💰 MARKET SELL - LIMIT BUY: {} @ {} - Sell#{} vs Buy#{}",
                    matchQuantity, tradePrice, marketSell.getId(), limitBuy.getId());

            executeTrade(limitBuy, marketSell, tradePrice, matchQuantity, TradeType.MARKET_LIMIT_SELL);
            remainingQuantity = remainingQuantity.subtract(matchQuantity);
        }

        updateOrderQuantity(marketSell, remainingQuantity);
    }

    private void updateOrderQuantity(OrderBooks order, BigDecimal remainingQuantity) {
        if (remainingQuantity.compareTo(order.getQuantity()) < 0) {
            order.setQuantity(remainingQuantity);
            order.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            orderBooksRepository.save(order);
        }
    }

    private void processLimitOrders(List<OrderBooks> buyOrders, List<OrderBooks> sellOrders, String symbol) {
        // Lọc chỉ limit orders còn active
        List<OrderBooks> limitBuyOrders = buyOrders.stream()
                .filter(OrderBooks::isLimitOrder)
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());

        List<OrderBooks> limitSellOrders = sellOrders.stream()
                .filter(OrderBooks::isLimitOrder)
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());

        log.debug("🔄 Processing {} limit buy and {} limit sell orders",
                limitBuyOrders.size(), limitSellOrders.size());

        // Sử dụng PriorityQueue cho limit order matching
        PriorityQueue<OrderBooks> buyQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o2.getPrice().compareTo(o1.getPrice()); // Giá cao nhất trước
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt()); // Cũ nhất trước
        });

        PriorityQueue<OrderBooks> sellQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o1.getPrice().compareTo(o2.getPrice()); // Giá thấp nhất trước
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt()); // Cũ nhất trước
        });

        buyQueue.addAll(limitBuyOrders);
        sellQueue.addAll(limitSellOrders);

        int matchCount = 0;
        while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
            OrderBooks bestBuy = buyQueue.peek();
            OrderBooks bestSell = sellQueue.peek();

            log.debug("⚖️  Comparing Limit Buy[{}@{}] vs Limit Sell[{}@{}]",
                    bestBuy.getId(), bestBuy.getPrice(),
                    bestSell.getId(), bestSell.getPrice());

            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0) {
                BigDecimal matchQuantity = bestBuy.getQuantity().min(bestSell.getQuantity());
                BigDecimal tradePrice = determineTradePrice(bestBuy, bestSell);

                log.info("🤝 LIMIT-LIMIT Match: {} @ {} between Buy#{} and Sell#{}",
                        matchQuantity, tradePrice, bestBuy.getId(), bestSell.getId());

                executeTrade(bestBuy, bestSell, tradePrice, matchQuantity, TradeType.LIMIT_LIMIT);
                matchCount++;

                // Remove filled orders from queues
                if (bestBuy.getStatus() == OrderStatus.DONE) {
                    buyQueue.poll();
                    log.debug("✅ Removed filled buy order: {}", bestBuy.getId());
                }
                if (bestSell.getStatus() == OrderStatus.DONE) {
                    sellQueue.poll();
                    log.debug("✅ Removed filled sell order: {}", bestSell.getId());
                }
            } else {
                log.debug("❌ No more limit matches possible");
                break;
            }
        }

        log.info("🎯 Limit matching completed. {} trades executed for symbol: {}", matchCount, symbol);
    }

    private void executeTrade(OrderBooks buyOrder, OrderBooks sellOrder, BigDecimal price, BigDecimal quantity,
            TradeType tradeType) {
        // Update order quantities
        BigDecimal newQuantity1 = buyOrder.getQuantity().subtract(quantity);
        BigDecimal newQuantity2 = sellOrder.getQuantity().subtract(quantity);
        LocalDateTime update_at = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        // Update buyOrder - CHỈ set DONE khi quantity = 0
        if (newQuantity1.compareTo(BigDecimal.ZERO) == 0) {
            buyOrder.setStatus(OrderStatus.DONE);
        }
        buyOrder.setUpdatedAt(update_at);
        buyOrder.setQuantity(newQuantity1);

        // Update sellOrder - CHỈ set DONE khi quantity = 0
        if (newQuantity2.compareTo(BigDecimal.ZERO) == 0) {
            sellOrder.setStatus(OrderStatus.DONE);
        }
        sellOrder.setUpdatedAt(update_at);
        sellOrder.setQuantity(newQuantity2);

        // Save orders
        orderBooksRepository.save(buyOrder);
        orderBooksRepository.save(sellOrder);

        spotHistoryService.createSpotRecord(
                buyOrder.getSymbol(),
                price,
                quantity,
                buyOrder.getId(),
                sellOrder.getId(),
                tradeType);
        // TODO: Create trade record
        log.info("💾 Saved matched orders: #{}, #{}", buyOrder.getId(), sellOrder.getId());
    }

    private BigDecimal determineTradePrice(OrderBooks buyOrder, OrderBooks sellOrder) {
        // Price-time priority: order nào đặt trước thì dùng giá của order đó
        return buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt()) ? buyOrder.getPrice() : sellOrder.getPrice();
    }

    // Helper method to get last traded price (cần implement thực tế)
    private BigDecimal getLastTradedPrice(String symbol) {
        // Trong thực tế, lấy từ database hoặc cache
        // Tạm thời return giá mặc định
        return BigDecimal.valueOf(1000); // Example price
    }
}