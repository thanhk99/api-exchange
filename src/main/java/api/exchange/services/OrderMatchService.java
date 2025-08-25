package api.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import api.exchange.config.OrderMatchingSchedule;
import api.exchange.models.OrderBooks;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMatchService {

    @Autowired
    private OrderBooksService orderBooksService;

    @Autowired
    private OrderMatchingSchedule orderMatchingSchedule;

    @Async
    @EventListener
    @Transactional
    public void handleNewOrder(OrderCreatedEvent event) {
        OrderBooks newOrderBooks = event.getOrder();
        log.info("🎯 Auto-matching triggered for order: {} ", newOrderBooks.getId());
        orderBooksService.matchOrders(newOrderBooks.getSymbol());
        orderMatchingSchedule.scheduleMatching(newOrderBooks.getSymbol());
    }

    public static class OrderCreatedEvent {
        private final OrderBooks order;

        public OrderCreatedEvent(OrderBooks order) {
            this.order = order;
        }

        public OrderBooks getOrder() {
            return order;
        }
    }
}
