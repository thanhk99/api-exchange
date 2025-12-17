package api.exchange.websocket;

import api.exchange.models.FuturesOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FuturesOrderWebSocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast order book update to all subscribers of a symbol
     * Topic: /topic/futures/orderbook/{symbol}
     */
    public void broadcastOrderBookUpdate(FuturesOrder order) {
        Map<String, Object> update = new HashMap<>();
        update.put("action", "UPDATE"); // or DELETE if filled/cancelled?
        update.put("symbol", order.getSymbol());
        update.put("side", order.getSide());
        update.put("price", order.getPrice());
        update.put("quantity", order.getQuantity()); // Remaining quantity
        update.put("status", order.getStatus());
        update.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/futures/orderbook/" + order.getSymbol(), update);
    }

    /**
     * Send order update to the specific user
     * Topic: /topic/futures/orders/{uid}
     */
    public void sendUserOrderUpdate(FuturesOrder order) {
        messagingTemplate.convertAndSend("/topic/futures/orders/" + order.getUid(), order);
    }
}
