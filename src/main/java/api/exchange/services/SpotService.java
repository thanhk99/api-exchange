package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.SpotWalletHistory;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.models.OrderBooks.TradeType;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.websocket.SpotOrderWebSocket;
import jakarta.transaction.Transactional;

@Service
public class SpotService {

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SpotWalletService spotWalletService;

    @Autowired
    private SpotOrderWebsocket spotOrderWebsocket;

    @Autowired
    private SpotOrderWebSocket spotOrderWebsocket;

    @Autowired
    private api.exchange.repository.SpotWalletRepository spotWalletRepository;

    @Transactional
    public ResponseEntity<?> createOrder(OrderBooks entity, String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        if (!spotWalletService.checkBalance(entity, uid)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Số dư không đủ"));
        }
        spotWalletService.checkWalletRecive(entity, uid);
        OrderBooks orderBooks = orderBooksRepository.findByUidAndSymbolAndPriceAndStatusAndOrderTypeAndTradeType(uid,
                entity.getSymbol(),
                entity.getPrice(), OrderStatus.ACTIVE, entity.getOrderType(), entity.getTradeType());
        LocalDateTime createAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (orderBooks != null) {
            orderBooks.setQuantity(orderBooks.getQuantity().add(entity.getQuantity()));
            orderBooks.setUpdatedAt(createAt);
            return ResponseEntity.ok(Map.of("message", "success", "data", "Cập nhật lệnh"));
        }
        entity.setStatus(OrderStatus.ACTIVE);
        entity.setUid(uid);
        entity.setCreatedAt(createAt);

        spotWalletService.lockBalanceLimit(entity, uid);

        BigDecimal lockAmount;
        String asset = "";
        if (entity.getOrderType().equals(OrderType.BUY)) {
            lockAmount = entity.getPrice().multiply(entity.getQuantity());
            asset = entity.getSymbol().split("/")[1];
        } else {
            lockAmount = entity.getQuantity();
            asset = entity.getSymbol().split("/")[0];
        }

        api.exchange.models.SpotWallet wallet = spotWalletRepository.findByUidAndCurrency(uid, asset);

        SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
        spotWalletHistory.setUserId(uid);
        spotWalletHistory.setAsset(asset);
        spotWalletHistory.setType("Tạo lệnh");
        spotWalletHistory.setAmount(lockAmount.negate());
        spotWalletHistory.setBalance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO);
        spotWalletHistory.setCreateDt(createAt);
        spotWalletHistoryRepository.save(spotWalletHistory);

        spotOrderWebsocket.broadcastOrderBooks(entity);

        OrderBooks orderSaved = orderBooksRepository.saveAndFlush(entity);
        orderBooksService.matchOrders(orderSaved);

        return ResponseEntity.ok(Map.of("message", "success", "data", "Tạo Order thành công "));
    }

    @Transactional
    public ResponseEntity<?> cancelOrder(Long orderId) {
        Optional<OrderBooks> orderOpt = orderBooksRepository.findById(orderId);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không tìm thấy id"));
        }

        OrderBooks order = orderOpt.get();

        if (order.getStatus() == OrderStatus.ACTIVE || order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            spotWalletService.unlockBalance(order, order.getUid());

            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            orderBooksRepository.save(order);
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không thể huỷ lệnh "));
    }

    public Map<String, Object> getOrderBook(String symbol, int limit) {
        // Get BUY LIMIT orders (bids), sorted by price descending (highest first)
        List<OrderBooks> buyOrders = orderBooksRepository.findBuyOrderBooks(symbol);

        // Get SELL LIMIT orders (asks), sorted by price ascending (lowest first)
        List<OrderBooks> sellOrders = orderBooksRepository.findSellOrderBooks(symbol);

        // Aggregate orders by price level
        List<List<String>> bids = aggregateOrdersByPrice(buyOrders, limit);
        List<List<String>> asks = aggregateOrdersByPrice(sellOrders, limit);

        return Map.of(
                "symbol", symbol,
                "timestamp", System.currentTimeMillis(),
                "bids", bids,
                "asks", asks);
    }

    private List<List<String>> aggregateOrdersByPrice(List<OrderBooks> orders, int limit) {
        Map<BigDecimal, BigDecimal> priceMap = new java.util.LinkedHashMap<>();

        for (OrderBooks order : orders) {
            // Only aggregate LIMIT orders that are not fully filled
            if (order.getTradeType().equals(TradeType.LIMIT)) {
                priceMap.merge(order.getPrice(), order.getRemainingQuantity(), BigDecimal::add);
            }
        }

        return priceMap.entrySet().stream()
                .limit(limit)
                .map(e -> java.util.Arrays.asList(
                        e.getKey().toPlainString(),
                        e.getValue().toPlainString()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lấy lịch sử lệnh của user (với filter)
     */
    public List<OrderBooks> getUserOrders(String header, String symbol, String status, int limit, int offset) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                offset / limit,
                limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        if (symbol != null && !symbol.isEmpty() && status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderBooksRepository.findByUidAndSymbolAndStatus(uid, symbol, orderStatus, pageable);
        } else if (symbol != null && !symbol.isEmpty()) {
            return orderBooksRepository.findByUidAndSymbol(uid, symbol, pageable);
        } else if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderBooksRepository.findByUidAndStatus(uid, orderStatus, pageable);
        } else {
            return orderBooksRepository.findByUid(uid, pageable);
        }
    }
}
