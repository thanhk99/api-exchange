package api.exchange.websocket;

import api.exchange.models.TransactionSpot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpotTradeWebSocket {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast public trade feed cho một symbol
     */
    public void broadcastTrade(TransactionSpot trade) {
        String symbol = trade.getSymbol().replace("USDT", "-USDT");
        String topic = "/topic/spot/trades/" + symbol.toLowerCase();

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("id", trade.getId());
        tradeData.put("symbol", trade.getSymbol());
        tradeData.put("price", trade.getPrice());
        tradeData.put("quantity", trade.getQuantity());
        tradeData.put("quoteQuantity", trade.getQuoteQuantity());
        tradeData.put("side", trade.getSide().toString());
        tradeData.put("isMaker", trade.getIsMaker());
        tradeData.put("executedAt", trade.getExecutedAt().toString());
        tradeData.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend(topic, tradeData);
    }

    /**
     * Broadcast batch trades (nhiều giao dịch cùng lúc)
     */
    public void broadcastTradeBatch(List<TransactionSpot> trades) {
        if (trades == null || trades.isEmpty())
            return;

        String symbol = trades.get(0).getSymbol().replace("USDT", "-USDT");
        String topic = "/topic/spot/trades/" + symbol.toLowerCase();

        messagingTemplate.convertAndSend(topic, trades);
    }
}
