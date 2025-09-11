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

import api.exchange.models.SpotWallet;
import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.models.OrderBooks.OrderType;
import api.exchange.models.OrderBooks.TradeType;
import api.exchange.repository.SpotWalletRepository;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;

@Service
public class SpotService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SpotWalletRepository spotWalletRepository;

    @Autowired
    private OrderBooksService orderBooksService;

    @Transactional
    public ResponseEntity<?> createOrder(OrderBooks entity, String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);
        if (!checkBalance(entity, uid)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Số dư không đủ"));
        }
        checkWalletRecive(entity, uid);
        OrderBooks orderBooks = orderBooksRepository.findByUidAndSymbolAndPriceAndStatusAndOrderTypeAndTradeType(uid,
                entity.getSymbol(),
                entity.getPrice(), OrderStatus.PENDING, entity.getOrderType(), entity.getTradeType());
        LocalDateTime createAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (orderBooks != null) {
            orderBooks.setQuantity(orderBooks.getQuantity().add(entity.getQuantity()));
            orderBooks.setUpdatedAt(createAt);
            return ResponseEntity.ok(Map.of("message", "success", "data", "Cập nhật lệnh"));
        }
        entity.setStatus(OrderStatus.PENDING);
        entity.setUid(uid);
        entity.setCreatedAt(createAt);

        lockBalanceLimit(entity, uid);

        orderBooksRepository.save(entity);

        eventPublisher.publishEvent(new OrderMatchService.OrderCreatedEvent(entity));

        return ResponseEntity.ok(Map.of("message", "success", "data", "Tạo Order thành công "));
    }

    @Transactional
    public ResponseEntity<?> cancleOrder(Long orderId) {
        Optional<OrderBooks> orderBooks = orderBooksRepository.findById(orderId);
        if (!orderBooks.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không tìm thấy id"));
        }
        if (orderBooks.get().getStatus() == OrderStatus.PENDING) {
            return ResponseEntity.ok(Map.of("message", "success", "data", "Huỷ lệnh thành công"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Bad Request", "data", "Không thể huỷ lệnh "));
    }

    public Boolean checkBalance(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];

        if (entity.getTradeType().equals(TradeType.MARKET) && entity.getOrderType().equals(OrderType.BUY)) {
            BigDecimal priceCoin = orderBooksService.getLastTradedPrice(entity.getSymbol());

            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
            if (spotWallet == null) {
                return false;
            }
            if (spotWallet.getBalance().compareTo(entity.getQuantity().multiply(priceCoin)) < 0) {
                return false;
            }
        } else {
            if (entity.getOrderType().equals(OrderType.BUY)) {
                SpotWallet fundingWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
                if (fundingWallet == null) {
                    return false;
                }
                if (fundingWallet.getBalance().compareTo(entity.getPrice().multiply(entity.getQuantity())) < 0) {
                    return false;
                }
            } else {
                SpotWallet fundingWallet = spotWalletRepository.findByUidAndCurrency(uid, coin);
                if (fundingWallet == null) {
                    return false;
                }
                if (fundingWallet.getBalance().compareTo(entity.getQuantity()) < 0) {
                    return false;
                }
            }

        }
        return true;
    }

    public void lockBalanceLimit(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];
        if (entity.getTradeType().equals(TradeType.LIMIT)) {
            if (entity.getOrderType().equals(OrderType.BUY)) {
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
                BigDecimal totalStableCoin = entity.getPrice().multiply(entity.getQuantity());
                spotWallet.setLockedBalance(spotWallet.getLockedBalance().add(totalStableCoin));
                spotWallet.setBalance(spotWallet.getBalance().subtract(totalStableCoin));
                spotWalletRepository.save(spotWallet);
            } else {
                SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, coin);
                spotWallet.setLockedBalance(spotWallet.getLockedBalance().add(entity.getQuantity()));
                spotWallet.setBalance(spotWallet.getBalance().subtract(entity.getQuantity()));
                spotWalletRepository.save(spotWallet);
            }
        }
    }
    
    @Transactional
    public void checkWalletRecive(OrderBooks entity, String uid) {
        String[] parts = entity.getSymbol().split("/");
        String coin = parts[0];
        String stable = parts[1];
        if(entity.getOrderType().equals(OrderType.BUY)) {
            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, coin);
            if (spotWallet == null) {
                SpotWallet newWallet = new SpotWallet();
                newWallet.setUid(uid);
                newWallet.setCurrency(coin);
                newWallet.setBalance(BigDecimal.ZERO);
                newWallet.setLockedBalance(BigDecimal.ZERO);
                newWallet.setActive(true);
                spotWalletRepository.save(newWallet);
            }
        }   else {
            SpotWallet spotWallet = spotWalletRepository.findByUidAndCurrency(uid, stable);
            if (spotWallet == null) {
                SpotWallet newWallet = new SpotWallet();
                newWallet.setUid(uid);
                newWallet.setCurrency(stable);
                newWallet.setBalance(BigDecimal.ZERO);
                newWallet.setLockedBalance(BigDecimal.ZERO);
                newWallet.setActive(true);
                spotWalletRepository.save(newWallet);
            }
        }
    }
}
