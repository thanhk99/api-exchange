package api.exchange.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import api.exchange.models.OrderBooks;
import java.util.HashMap;
import java.util.Map;

public class SpotOrderWebsocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastOrderBooks(OrderBooks order) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("type", order.getOrderType());
        orderData.put("price", order.getPrice());
        orderData.put("quanlity", order.getQuantity());
        orderData.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/spot/orderbook/" + order.getSymbol(), orderData);
    }
}