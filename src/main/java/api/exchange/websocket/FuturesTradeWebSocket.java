package api.exchange.websocket;

import api.exchange.models.FuturesTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FuturesTradeWebSocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast public trade feed cho Futures
     */
    public void broadcastTrade(String symbol, Map<String, Object> tradeData) {
        String formattedSymbol = symbol.replace("USDT", "-USDT");
        String topic = "/topic/futures/trades/" + formattedSymbol.toLowerCase();

        messagingTemplate.convertAndSend(topic, tradeData);
    }

    /**
     * Broadcast user-specific position update
     */
    public void broadcastPositionUpdate(String uid, Map<String, Object> positionData) {
        String topic = "/user/" + uid + "/queue/futures/positions";
        messagingTemplate.convertAndSend(topic, positionData);
    }

    /**
     * Broadcast user-specific order update
     */
    public void broadcastOrderUpdate(String uid, Map<String, Object> orderData) {
        String topic = "/user/" + uid + "/queue/futures/orders";
        messagingTemplate.convertAndSend(topic, orderData);
    }
}
