package api.exchange.services;

import api.exchange.models.*;
import api.exchange.repository.FuturesOrderRepository;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.FuturesWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FuturesOrderService {

    @Autowired
    private FuturesOrderRepository futuresOrderRepository;

    @Autowired
    private FuturesTradingService futuresTradingService;

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private CoinDataService coinDataService;

    // ==================== SCHEDULED MATCHING ====================

    @Scheduled(fixedRate = 1000) // Run every second
    @Transactional
    public void matchLimitOrders() {
        // P2P Matching Engine Trigger
        // Hardcoded symbols for MVP or fetch from DB
        List<String> activeSymbols = Arrays.asList("BTCUSDT", "ETHUSDT", "BNBUSDT");

        for (String symbol : activeSymbols) {
            futuresTradingService.matchOrders(symbol);
        }
    }

    public Map<String, Object> getOrderBook(String symbol, int limit) {
        List<FuturesOrder> buyOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusAndType(
                        symbol,
                        FuturesOrder.OrderSide.BUY,
                        FuturesOrder.OrderStatus.PENDING,
                        FuturesOrder.OrderType.LIMIT,
                        PageRequest.of(0, limit, Sort.by("price").descending()));

        List<FuturesOrder> sellOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusAndType(
                        symbol,
                        FuturesOrder.OrderSide.SELL,
                        FuturesOrder.OrderStatus.PENDING,
                        FuturesOrder.OrderType.LIMIT,
                        PageRequest.of(0, limit, Sort.by("price").ascending()));

        List<List<String>> bids = aggregateOrdersByPrice(buyOrders);
        List<List<String>> asks = aggregateOrdersByPrice(sellOrders);

        return Map.of(
                "symbol", symbol,
                "lastUpdateId", System.currentTimeMillis(),
                "bids", bids,
                "asks", asks);
    }

    public List<FuturesOrder> getOrders(String uid, String symbol, String status, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("createdAt").descending());

        FuturesOrder.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = FuturesOrder.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        if (uid != null && !uid.isEmpty()) {
            if (symbol != null && !symbol.isEmpty() && orderStatus != null) {
                return futuresOrderRepository.findByUidAndSymbolAndStatusOrderByCreatedAtDesc(uid, symbol, orderStatus,
                        pageable);
            } else if (symbol != null && !symbol.isEmpty()) {
                return futuresOrderRepository.findByUidAndSymbolOrderByCreatedAtDesc(uid, symbol, pageable);
            } else if (orderStatus != null) {
                return futuresOrderRepository.findByUidAndStatusOrderByCreatedAtDesc(uid, orderStatus, pageable);
            } else {
                return futuresOrderRepository.findByUidOrderByCreatedAtDesc(uid, pageable);
            }
        } else {
            if (symbol != null && !symbol.isEmpty() && orderStatus != null) {
                return futuresOrderRepository.findBySymbolAndStatusOrderByCreatedAtDesc(symbol, orderStatus, pageable);
            } else if (symbol != null && !symbol.isEmpty()) {
                return futuresOrderRepository.findBySymbolOrderByCreatedAtDesc(symbol, pageable);
            } else {
                return futuresOrderRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        }
    }

    // ==================== AUTHENTICATED METHODS ====================

    /**
     * Place a new order (Authenticated)
     */
    @Transactional
    public FuturesOrder placeOrder(String uid, String symbol, FuturesOrder.OrderSide side,
            FuturesOrder.PositionSide positionSide,
            FuturesOrder.OrderType type, BigDecimal price, BigDecimal quantity, int leverage) {

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (leverage < 1 || leverage > 125) {
            throw new IllegalArgumentException("Invalid leverage");
        }

        BigDecimal currentPrice = coinDataService.getCurrentPrice(symbol);
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Price unavailable for symbol: " + symbol);
        }

        BigDecimal executionPrice = (type == FuturesOrder.OrderType.MARKET) ? currentPrice : price;

        // Self-Trade Prevention (STP) Check
        if (type == FuturesOrder.OrderType.LIMIT) {
            if (side == FuturesOrder.OrderSide.BUY) {
                // Check if I have a SELL order with Price <= My Price
                boolean conflict = futuresOrderRepository
                        .existsByUidAndSymbolAndSideAndStatusAndPriceLessThanEqual(
                                uid, symbol, FuturesOrder.OrderSide.SELL, FuturesOrder.OrderStatus.PENDING, price);
                if (conflict) {
                    throw new RuntimeException(
                            "Self-trade prevention: You have a Sell order with lower or equal price.");
                }
            } else {
                // Check if I have a BUY order with Price >= My Price
                boolean conflict = futuresOrderRepository
                        .existsByUidAndSymbolAndSideAndStatusAndPriceGreaterThanEqual(
                                uid, symbol, FuturesOrder.OrderSide.BUY, FuturesOrder.OrderStatus.PENDING, price);
                if (conflict) {
                    throw new RuntimeException(
                            "Self-trade prevention: You have a Buy order with higher or equal price.");
                }
            }
        }

        BigDecimal notionalValue = executionPrice.multiply(quantity);
        BigDecimal requiredMargin = notionalValue.divide(BigDecimal.valueOf(leverage), 8, RoundingMode.HALF_UP);

        FuturesWallet wallet = futuresWalletRepository.findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Futures wallet not found"));

        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (availableBalance.compareTo(requiredMargin) < 0) {
            throw new RuntimeException("Insufficient margin");
        }

        FuturesOrder order = new FuturesOrder();
        order.setUid(uid);
        order.setSymbol(symbol);
        order.setSide(side);
        order.setPositionSide(positionSide);
        order.setType(type);
        order.setPrice(executionPrice);
        order.setQuantity(quantity);
        order.setLeverage(leverage);
        order.setStatus(FuturesOrder.OrderStatus.PENDING);

        wallet.setLockedBalance(wallet.getLockedBalance().add(requiredMargin));
        futuresWalletRepository.save(wallet);

        futuresOrderRepository.save(order);

        // Trigger Matching Engine
        futuresTradingService.matchOrders(symbol);

        return order;
    }

    /**
     * Cancel a pending order (Authenticated)
     */
    @Transactional
    public void cancelOrder(String uid, Long orderId) {
        FuturesOrder order = futuresOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUid().equals(uid)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        if (order.getStatus() != FuturesOrder.OrderStatus.PENDING &&
                order.getStatus() != FuturesOrder.OrderStatus.PARTIALLY_FILLED) {
            throw new RuntimeException("Cannot cancel order in current status: " + order.getStatus());
        }

        FuturesWallet wallet = futuresWalletRepository
                .findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal notionalValue = order.getPrice().multiply(order.getQuantity());
        BigDecimal lockedMargin = notionalValue.divide(
                BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(lockedMargin));
        if (wallet.getLockedBalance().compareTo(BigDecimal.ZERO) < 0) {
            wallet.setLockedBalance(BigDecimal.ZERO);
        }
        futuresWalletRepository.save(wallet);

        order.setStatus(FuturesOrder.OrderStatus.CANCELLED);
        futuresOrderRepository.save(order);
    }

    // ==================== HELPER METHODS ====================

    private List<List<String>> aggregateOrdersByPrice(List<FuturesOrder> orders) {
        Map<BigDecimal, BigDecimal> priceMap = new LinkedHashMap<>();

        for (FuturesOrder order : orders) {
            priceMap.merge(order.getPrice(), order.getQuantity(), BigDecimal::add);
        }

        return priceMap.entrySet().stream()
                .map(e -> Arrays.asList(
                        e.getKey().toPlainString(),
                        e.getValue().toPlainString()))
                .collect(Collectors.toList());
    }
}
