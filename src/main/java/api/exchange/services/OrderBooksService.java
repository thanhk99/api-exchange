package api.exchange.services;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.repository.OrderBooksRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderBooksService {

    @Autowired
    private OrderBooksRepository orderBooksRepository;

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
                .collect(Collectors.toList());

        List<OrderBooks> marketSellOrders = sellOrders.stream()
                .filter(OrderBooks::isMarketOrder)
                .collect(Collectors.toList());

        log.debug("🔄 Processing {} market buy and {} market sell orders",
                marketBuyOrders.size(), marketSellOrders.size());

        // Xử lý market buy orders với limit sell orders
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

            matchMarketOrderWithLimitOrders(marketBuy, availableSellOrders, true);
        }

        // Xử lý market sell orders với limit buy orders
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

            matchMarketOrderWithLimitOrders(marketSell, availableBuyOrders, false);
        }
    }

    private void matchMarketOrderWithLimitOrders(OrderBooks marketOrder,
            List<OrderBooks> limitOrders,
            boolean isBuyMarket) {
        BigDecimal remainingQuantity = marketOrder.getQuantity();

        for (OrderBooks limitOrder : limitOrders) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (limitOrder.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            BigDecimal matchQuantity = remainingQuantity.min(limitOrder.getQuantity());
            BigDecimal tradePrice = limitOrder.getPrice(); // Market order takes limit price

            log.info("🎯 MARKET-LIMIT Match: {} @ {} between Market#{} and Limit#{}",
                    matchQuantity, tradePrice, marketOrder.getId(), limitOrder.getId());

            executeTrade(marketOrder, limitOrder, tradePrice, matchQuantity);

            remainingQuantity = remainingQuantity.subtract(matchQuantity);
        }

        // Update market order status
        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            marketOrder.setStatus(OrderStatus.DONE);
        } else if (remainingQuantity.compareTo(marketOrder.getQuantity()) < 0) {
            marketOrder.setStatus(OrderStatus.DONE);
            marketOrder.setQuantity(remainingQuantity);
        }

        orderBooksRepository.save(marketOrder);
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

                executeTrade(bestBuy, bestSell, tradePrice, matchQuantity);
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

    private void executeTrade(OrderBooks order1, OrderBooks order2, BigDecimal price, BigDecimal quantity) {
        // Update order quantities
        BigDecimal newQuantity1 = order1.getQuantity().subtract(quantity);
        BigDecimal newQuantity2 = order2.getQuantity().subtract(quantity);

        // Update order1
        if (newQuantity1.compareTo(BigDecimal.ZERO) == 0) {
            order1.setStatus(OrderStatus.DONE);
        } else if (newQuantity1.compareTo(order1.getQuantity()) < 0) {
            order1.setStatus(OrderStatus.DONE);
        }
        order1.setQuantity(newQuantity1);

        // Update order2
        if (newQuantity2.compareTo(BigDecimal.ZERO) == 0) {
            order2.setStatus(OrderStatus.DONE);
        } else if (newQuantity2.compareTo(order2.getQuantity()) < 0) {
            order2.setStatus(OrderStatus.DONE);
        }
        order2.setQuantity(newQuantity2);

        // Save orders
        orderBooksRepository.save(order1);
        orderBooksRepository.save(order2);

        // TODO: Create trade record
        log.info("💾 Saved matched orders: #{}, #{}", order1.getId(), order2.getId());
    }

    private BigDecimal determineTradePrice(OrderBooks buyOrder, OrderBooks sellOrder) {
        // Price-time priority: order nào đặt trước thì dùng giá của order đó
        return buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt()) ? buyOrder.getPrice() : sellOrder.getPrice();
    }
}