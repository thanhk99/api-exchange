package api.exchange.services;

import api.exchange.config.RabbitMQConfig;
import api.exchange.models.OrderBooks;
import api.exchange.repository.OrderBooksRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SpotMatchingListener {

    @Autowired
    private OrderBooksService orderBooksService;

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, concurrency = "1-1")
    public void receiveMessage(Long orderId) {
        log.info("📥 Received match request for Order ID: {}", orderId);
        try {
            OrderBooks order = orderBooksRepository.findById(orderId).orElse(null);
            if (order != null) {
                orderBooksService.matchOrders(order);
            } else {
                log.warn("⚠️ Order not found for ID: {}", orderId);
            }
        } catch (Exception e) {
            log.error("❌ Error processing match for order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
