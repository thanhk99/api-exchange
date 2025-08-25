package api.exchange.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.PriorityQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.repository.OrderBooksRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderBooksService {

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ✅ Method để trigger matching từ bên ngoài
    @Transactional
    public void triggerMatching(String symbol) {
        log.info("🚀 Triggering matching for symbol: {}", symbol);
        matchOrders(symbol);
    }

    @Transactional
    public void matchOrders(String symbol) {
        log.debug("🔍 Starting matching for symbol: {}", symbol);

        List<OrderBooks> buyOrders = orderBooksRepository.findBuyOrderBooks(symbol);
        List<OrderBooks> sellOrders = orderBooksRepository.findSellOrderBooks(symbol);

        log.debug("📊 Buy orders: {}, Sell orders: {}", buyOrders.size(), sellOrders.size());

        PriorityQueue<OrderBooks> buyQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o2.getPrice().compareTo(o1.getPrice());
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });

        PriorityQueue<OrderBooks> sellQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o1.getPrice().compareTo(o2.getPrice());
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });

        buyQueue.addAll(buyOrders);
        sellQueue.addAll(sellOrders);

        int matchCount = 0;
        while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
            OrderBooks bestBuy = buyQueue.peek();
            OrderBooks bestSell = sellQueue.peek();

            log.debug("⚖️  Comparing Buy[{}@{}] vs Sell[{}@{}]",
                    bestBuy.getId(), bestBuy.getPrice(),
                    bestSell.getId(), bestSell.getPrice());

            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0) {
                matchOrder(bestBuy, bestSell);
                matchCount++;

                // ✅ Remove cả hai orders nếu đã filled
                if (bestBuy.getStatus() == OrderStatus.DONE) {
                    buyQueue.poll();
                    log.debug("✅ Removed filled buy order: {}", bestBuy.getId());
                }
                if (bestSell.getStatus() == OrderStatus.DONE) {
                    sellQueue.poll();
                    log.debug("✅ Removed filled sell order: {}", bestSell.getId());
                }
            } else {
                log.debug("❌ No more matches possible");
                break;
            }
        }

        log.info("🎯 Matching completed. {} trades executed for symbol: {}", matchCount, symbol);
    }

    @Transactional
    public void matchOrder(OrderBooks buyOrder, OrderBooks sellOrder) {
        BigDecimal matchQuantity = buyOrder.getQuantity().min(sellOrder.getQuantity());
        BigDecimal tradePrice = determineTradePrice(buyOrder, sellOrder);

        log.info("🤝 MATCHING: {} @ {} between Buy#{} and Sell#{}",
                matchQuantity, tradePrice, buyOrder.getId(), sellOrder.getId());

        // Update buy order
        BigDecimal newBuyQuantity = buyOrder.getQuantity().subtract(matchQuantity);
        if (newBuyQuantity.compareTo(BigDecimal.ZERO) == 0) {
            buyOrder.setStatus(OrderStatus.DONE);
        }
        buyOrder.setQuantity(newBuyQuantity);

        // Update sell order
        BigDecimal newSellQuantity = sellOrder.getQuantity().subtract(matchQuantity);
        if (newSellQuantity.compareTo(BigDecimal.ZERO) == 0) {
            sellOrder.setStatus(OrderStatus.DONE);
        }
        sellOrder.setQuantity(newSellQuantity);

        orderBooksRepository.save(buyOrder);
        orderBooksRepository.save(sellOrder);

        // TODO: Create trade record here
        log.info("💾 Saved matched orders: Buy#{}, Sell#{}", buyOrder.getId(), sellOrder.getId());
    }

    private BigDecimal determineTradePrice(OrderBooks buyOrder, OrderBooks sellOrder) {
        // Price-time priority
        return buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt()) ? buyOrder.getPrice() : sellOrder.getPrice();
    }
}