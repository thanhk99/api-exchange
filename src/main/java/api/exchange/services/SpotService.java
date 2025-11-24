package api.exchange.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.SpotWalletHistory;
import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.repository.SpotWalletHistoryRepository;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.websocket.SpotOrderWebsocket;
import jakarta.transaction.Transactional;

@Service
public class SpotService {

    @Autowired
    private SpotWalletHistoryRepository spotWalletHistoryRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SpotWalletService spotWalletService;

    @Autowired
    private OrderBooksService orderBooksService;

    @Autowired
    private SpotOrderWebsocket spotOrderWebsocket;

    SpotService(SpotWalletHistoryRepository spotWalletHistoryRepository) {
        this.spotWalletHistoryRepository = spotWalletHistoryRepository;
    }

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

        BigDecimal balance;
        String asset = "";
        if (entity.getOrderType().equals(OrderType.BUY)) {
            balance = entity.getPrice().multiply(entity.getQuantity());
            asset = entity.getSymbol().split("/")[1];
        } else {
            balance = entity.getQuantity();
            asset = entity.getSymbol().split("/")[0];
        }
        SpotWalletHistory spotWalletHistory = new SpotWalletHistory();
        spotWalletHistory.setUserId(uid);
        spotWalletHistory.setAsset(asset);
        spotWalletHistory.setType("Tạo lệnh");
        spotWalletHistory.setBalance(balance);
        spotWalletHistory.setCreateDt(createAt);
        spotWalletHistoryRepository.save(spotWalletHistory);

        spotOrderWebsocket.broadcastOrderBooks(entity);

        OrderBooks orderSaved = orderBooksRepository.saveAndFlush(entity);
        orderBooksService.matchOrders(orderSaved);
        // eventPublisher.publishEvent(new OrderMatchService.OrderCreatedEvent(entity));

        return ResponseEntity.ok(Map.of("message", "success", "data", "Tạo Order thành công "));
    }

    @Transactional
    public ResponseEntity<?> cancleOrder(Long orderId) {
        Optional<OrderBooks> orderOpt = orderBooksRepository.findById(orderId);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không tìm thấy id"));
        }

        OrderBooks order = orderOpt.get();

        if (orderOpt.get().getStatus() == OrderStatus.ACTIVE || order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            // For example: unlockBalance(order);
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            orderBooksRepository.save(order);
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không thể huỷ lệnh "));
    }

    // Methods moved to SpotWalletService

}
