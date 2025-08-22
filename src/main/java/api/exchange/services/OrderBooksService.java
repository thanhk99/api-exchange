package api.exchange.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.repository.OrderBooksRepository;
import java.util.PriorityQueue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderBooksService {

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Transactional
    public void matchOrders(String symbol) {
        List<OrderBooks> buyOrders = orderBooksRepository.findBuyOrders(symbol);
        List<OrderBooks> sellOrders = orderBooksRepository.findSellOrders(symbol);
        
        PriorityQueue<OrderBooks> buyQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o2.getPrice().compareTo(o1.getPrice());
            if (priceCompare != 0) return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });

        PriorityQueue<OrderBooks> sellQueue = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o1.getPrice().compareTo(o2.getPrice());
            if (priceCompare != 0) return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });

        buyQueue.addAll(buyOrders);
        sellQueue.addAll(sellOrders);

        while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
            OrderBooks bestBuy = buyQueue.peek();
            OrderBooks bestSell = sellQueue.peek();
            
            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0) {
                // Matching possible
                matchOrder(bestBuy, bestSell);
                
                // Remove filled orders from queues
                if (bestBuy.getStatus() == OrderStatus.DONE) {
                    buyQueue.poll();
                }
            } else {
                // No more matches possible
                break;
            }
        }
    }

    @Transactional
    public void matchOrder(OrderBooks buyOrder, OrderBooks sellOrder) {
        BigDecimal matchQuantity = buyOrder.getQuantity().min(sellOrder.getQuantity());
        BigDecimal tradePrice = determineTradePrice(buyOrder, sellOrder);

        log.debug("Matching {} @ {} between buyOrder {} and sellOrder {}", 
            matchQuantity, tradePrice, buyOrder.getId(), sellOrder.getId());

        // Update order quantities and status
        updateOrderStatus(buyOrder, matchQuantity);
        updateOrderStatus(sellOrder, matchQuantity);
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
    }

    private BigDecimal determineTradePrice(OrderBooks buyOrder, OrderBooks sellOrder) {
        // Price time priority: use the order that was placed first
        if (buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt())) {
            return buyOrder.getPrice();
        } else {
            return sellOrder.getPrice();
        }
    }

    private void updateOrderStatus(OrderBooks order, BigDecimal matchQuantity) {
        BigDecimal newQuantity = order.getQuantity().subtract(matchQuantity);
        order.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.DONE);
        }
    }

}
