package api.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

// import api.exchange.config.OrderMatchingSchedule;
import api.exchange.models.OrderBooks;
import jakarta.transaction.Transactional;

public class OrderMatchService {

    @Autowired
    private OrderBooksService orderBooksService;

    // @Autowired
    // private OrderMatchingSchedule orderMatchingSchedule;

    @Async 
    @EventListener
    @Transactional
    public void handleNewOrder(OrderCreatedEvent event){
        OrderBooks newOrderBooks = event.getOrder();
        orderBooksService.matchOrders(newOrderBooks.getSymbol());
        // orderMatchingSchedule.scheduleMatching(newOrderBooks.getSymbol());
    }
    public static class OrderCreatedEvent {
        private final OrderBooks orderbBooks;
        
        public OrderCreatedEvent(OrderBooks orderbBooks) {
            this.orderbBooks = orderbBooks;
        }
        
        public OrderBooks getOrder() {
            return orderbBooks;
        }
    }
}
