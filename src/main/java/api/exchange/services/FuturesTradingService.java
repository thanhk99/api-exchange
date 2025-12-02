package api.exchange.services;

import api.exchange.models.*;
import api.exchange.repository.FuturesOrderRepository;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.FuturesWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FuturesTradingService {

    @Autowired
    private FuturesOrderRepository futuresOrderRepository;

    @Autowired
    private FuturesPositionRepository futuresPositionRepository;

    @Autowired
    private FuturesWalletRepository futuresWalletRepository;

    @Autowired
    private CoinDataService coinDataService;

    @Transactional
    public FuturesOrder placeOrder(String uid, String symbol, FuturesOrder.OrderSide side,
            FuturesOrder.PositionSide positionSide,
            FuturesOrder.OrderType type, BigDecimal price, BigDecimal quantity, int leverage) {

        // 1. Validate inputs
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (leverage < 1 || leverage > 125) {
            throw new IllegalArgumentException("Invalid leverage");
        }

        // 2. Get Current Price (Mark Price)
        BigDecimal currentPrice = coinDataService.getCurrentPrice(symbol);
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Price unavailable for symbol: " + symbol);
        }

        // 3. Calculate Required Margin
        // Initial Margin = (Price * Quantity) / Leverage
        BigDecimal executionPrice = (type == FuturesOrder.OrderType.MARKET) ? currentPrice : price;
        BigDecimal notionalValue = executionPrice.multiply(quantity);
        BigDecimal requiredMargin = notionalValue.divide(BigDecimal.valueOf(leverage), 8, RoundingMode.HALF_UP);

        // 4. Check Wallet Balance
        FuturesWallet wallet = futuresWalletRepository.findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Futures wallet not found"));

        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (availableBalance.compareTo(requiredMargin) < 0) {
            throw new RuntimeException("Insufficient margin");
        }

        // 5. Create Order
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

        // 6. Lock Margin
        wallet.setLockedBalance(wallet.getLockedBalance().add(requiredMargin));
        futuresWalletRepository.save(wallet);

        futuresOrderRepository.save(order);

        // 7. Trigger Matching Engine
        matchOrders(symbol);

        return order;
    }

    @Transactional
    public void executeTrade(FuturesOrder makerOrder, FuturesOrder takerOrder, BigDecimal price, BigDecimal quantity) {
        // 1. Update Orders
        updateOrderAfterTrade(makerOrder, price, quantity);
        updateOrderAfterTrade(takerOrder, price, quantity);

        // 2. Update Positions for Maker
        updatePosition(makerOrder, price, quantity);

        // 3. Update Positions for Taker
        updatePosition(takerOrder, price, quantity);
    }

    private void updateOrderAfterTrade(FuturesOrder order, BigDecimal price, BigDecimal quantity) {
        BigDecimal remaining = order.getQuantity().subtract(quantity);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(FuturesOrder.OrderStatus.FILLED);
        } else {
            order.setStatus(FuturesOrder.OrderStatus.PARTIALLY_FILLED);
        }
        order.setQuantity(remaining); // Update remaining quantity
        // Note: For simplicity, we don't track cumulative filled quantity/avg price in
        // Order model yet.
        // In a real system, we would have 'filledQuantity' and 'averagePrice' fields.
        futuresOrderRepository.save(order);

        // Release Margin for the FILLED portion
        FuturesWallet wallet = futuresWalletRepository
                .findByUidAndCurrency(order.getUid(), "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal tradeValue = price.multiply(quantity);
        BigDecimal marginToRelease = tradeValue.divide(BigDecimal.valueOf(order.getLeverage()), 8,
                RoundingMode.HALF_UP);

        // Note: The margin locked was based on Order Price, but we release based on
        // Trade Price?
        // NO. We must release the margin that was originally locked.
        // Original Locked = OrderPrice * Qty / Leverage
        // We should release: OrderPrice * TradeQty / Leverage
        BigDecimal originalMarginLocked = order.getPrice().multiply(quantity)
                .divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(originalMarginLocked));
        if (wallet.getLockedBalance().compareTo(BigDecimal.ZERO) < 0) {
            wallet.setLockedBalance(BigDecimal.ZERO);
        }
        futuresWalletRepository.save(wallet);
    }

    private void updatePosition(FuturesOrder order, BigDecimal price, BigDecimal quantity) {
        Optional<FuturesPosition> existingPosition = futuresPositionRepository.findByUidAndSymbolAndStatus(
                order.getUid(), order.getSymbol(), FuturesPosition.PositionStatus.OPEN);

        FuturesWallet wallet = futuresWalletRepository
                .findByUidAndCurrency(order.getUid(), "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        FuturesPosition position;
        if (existingPosition.isPresent()) {
            position = existingPosition.get();
            if (position.getSide().name().equals(order.getPositionSide().name())) {
                // Add to position
                BigDecimal totalQuantity = position.getQuantity().add(quantity);
                BigDecimal totalCost = position.getQuantity().multiply(position.getEntryPrice())
                        .add(quantity.multiply(price));
                BigDecimal newEntryPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);

                position.setEntryPrice(newEntryPrice);
                position.setQuantity(totalQuantity);

                // Update Margin
                BigDecimal notionalValue = price.multiply(quantity);
                BigDecimal addedMargin = notionalValue.divide(BigDecimal.valueOf(order.getLeverage()), 8,
                        RoundingMode.HALF_UP);
                position.setMargin(position.getMargin().add(addedMargin));

                // Lock margin for the added position size
                wallet.setLockedBalance(wallet.getLockedBalance().add(addedMargin));
            } else {
                // Reduce/Close position (Hedge mode logic or simple close)
                // For P2P MVP, let's assume One-Way Mode for simplicity or throw error
                throw new RuntimeException("Opposite side position handling not implemented in MVP");
            }
        } else {
            // Create new position
            position = new FuturesPosition();
            position.setUid(order.getUid());
            position.setSymbol(order.getSymbol());
            position.setSide(FuturesPosition.PositionSide.valueOf(order.getPositionSide().name()));
            position.setEntryPrice(price);
            position.setQuantity(quantity);
            position.setLeverage(order.getLeverage());
            position.setStatus(FuturesPosition.PositionStatus.OPEN);

            BigDecimal notionalValue = price.multiply(quantity);
            BigDecimal margin = notionalValue.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);
            position.setMargin(margin);

            // Lock margin for the new position
            wallet.setLockedBalance(wallet.getLockedBalance().add(margin));

            // Calculate Liquidation Price
            if (position.getSide() == FuturesPosition.PositionSide.LONG) {
                BigDecimal liqPrice = price.multiply(BigDecimal.ONE.subtract(
                        BigDecimal.ONE.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP)));
                position.setLiquidationPrice(liqPrice);
            } else {
                BigDecimal liqPrice = price.multiply(BigDecimal.ONE
                        .add(BigDecimal.ONE.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP)));
                position.setLiquidationPrice(liqPrice);
            }
        }
        futuresWalletRepository.save(wallet);
        futuresPositionRepository.save(position);
    }

    // Additional methods for Cancel Order, Close Position, etc. would go here

    @Transactional
    public void closePosition(String uid, String symbol) {
        FuturesPosition position = futuresPositionRepository
                .findByUidAndSymbolAndStatus(uid, symbol, FuturesPosition.PositionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        BigDecimal currentPrice = coinDataService.getCurrentPrice(symbol);
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Price unavailable");
        }

        // Calculate PnL
        // Long: (Exit - Entry) * Qty
        // Short: (Entry - Exit) * Qty
        BigDecimal pnl;
        if (position.getSide() == FuturesPosition.PositionSide.LONG) {
            pnl = currentPrice.subtract(position.getEntryPrice()).multiply(position.getQuantity());
        } else {
            pnl = position.getEntryPrice().subtract(currentPrice).multiply(position.getQuantity());
        }

        // Update Wallet
        FuturesWallet wallet = futuresWalletRepository.findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Release Margin
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(position.getMargin()));

        // Add Margin + PnL to Balance
        // Note: Balance was already deducted by Margin when position opened (actually
        // locked).
        // Wait, in my design: Balance = Total, Locked = part of Total.
        // So Available = Balance - Locked.
        // When closing:
        // 1. Unlock Margin (Locked -= Margin)
        // 2. Add PnL to Balance (Balance += PnL)

        if (wallet.getLockedBalance().compareTo(BigDecimal.ZERO) < 0) {
            wallet.setLockedBalance(BigDecimal.ZERO); // Safety check
        }

        wallet.setBalance(wallet.getBalance().add(pnl));
        futuresWalletRepository.save(wallet);

        // Close Position
        position.setStatus(FuturesPosition.PositionStatus.CLOSED);
        futuresPositionRepository.save(position);

        // Record Transaction (Realized PnL)
        // We could record a transaction here for history
    }

    @Transactional
    public void adjustLeverage(String uid, String symbol, int newLeverage) {
        if (newLeverage < 1 || newLeverage > 125) {
            throw new IllegalArgumentException("Invalid leverage");
        }

        FuturesPosition position = futuresPositionRepository
                .findByUidAndSymbolAndStatus(uid, symbol, FuturesPosition.PositionStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        // Update Leverage
        position.setLeverage(newLeverage);

        // Recalculate Margin
        // Margin = (EntryPrice * Qty) / Leverage
        BigDecimal notionalValue = position.getEntryPrice().multiply(position.getQuantity());
        BigDecimal newMargin = notionalValue.divide(BigDecimal.valueOf(newLeverage), 8, RoundingMode.HALF_UP);

        BigDecimal marginDiff = newMargin.subtract(position.getMargin());

        FuturesWallet wallet = futuresWalletRepository.findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Check if we need to add more margin
        if (marginDiff.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedBalance());
            if (availableBalance.compareTo(marginDiff) < 0) {
                throw new RuntimeException("Insufficient balance to increase margin for lower leverage");
            }
        }

        // Update Wallet Locked Balance
        wallet.setLockedBalance(wallet.getLockedBalance().add(marginDiff));
        futuresWalletRepository.save(wallet);

        // Update Position Margin & Liquidation Price
        position.setMargin(newMargin);

        // Recalculate Liq Price
        if (position.getSide() == FuturesPosition.PositionSide.LONG) {
            BigDecimal liqPrice = position.getEntryPrice().multiply(BigDecimal.ONE
                    .subtract(BigDecimal.ONE.divide(BigDecimal.valueOf(newLeverage), 8, RoundingMode.HALF_UP)));
            position.setLiquidationPrice(liqPrice);
        } else {
            BigDecimal liqPrice = position.getEntryPrice().multiply(BigDecimal.ONE
                    .add(BigDecimal.ONE.divide(BigDecimal.valueOf(newLeverage), 8, RoundingMode.HALF_UP)));
            position.setLiquidationPrice(liqPrice);
        }

        futuresPositionRepository.save(position);
    }

    /**
     * Get user's orders with optional filtering
     */
    public List<FuturesOrder> getOrders(String uid, String symbol, String status, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);

        // Parse status if provided
        FuturesOrder.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = FuturesOrder.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        // Query based on filters
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
    }

    /**
     * Cancel a pending order (soft delete - only changes status to CANCELLED)
     * Does NOT delete the order record from database
     */
    @Transactional
    public void cancelOrder(String uid, Long orderId) {
        // 1. Find order
        FuturesOrder order = futuresOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Check ownership
        if (!order.getUid().equals(uid)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        // 3. Check status - can only cancel PENDING or PARTIALLY_FILLED orders
        if (order.getStatus() != FuturesOrder.OrderStatus.PENDING &&
                order.getStatus() != FuturesOrder.OrderStatus.PARTIALLY_FILLED) {
            throw new RuntimeException("Cannot cancel order in current status: " + order.getStatus());
        }

        // 4. Release margin
        FuturesWallet wallet = futuresWalletRepository
                .findByUidAndCurrency(uid, "USDT")
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal notionalValue = order.getPrice().multiply(order.getQuantity());
        BigDecimal lockedMargin = notionalValue.divide(
                BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(lockedMargin));
        if (wallet.getLockedBalance().compareTo(BigDecimal.ZERO) < 0) {
            wallet.setLockedBalance(BigDecimal.ZERO); // Safety check
        }
        futuresWalletRepository.save(wallet);

        // 5. Update order status
        order.setStatus(FuturesOrder.OrderStatus.CANCELLED);
        futuresOrderRepository.save(order);
    }

    /**
     * Get order book for a symbol
     */
    public Map<String, Object> getOrderBook(String symbol, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // Get BUY LIMIT orders (bids), sorted by price descending (highest first)
        List<FuturesOrder> buyOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusAndType(
                        symbol,
                        FuturesOrder.OrderSide.BUY,
                        FuturesOrder.OrderStatus.PENDING,
                        FuturesOrder.OrderType.LIMIT,
                        PageRequest.of(0, limit, Sort.by("price").descending()));

        // Get SELL LIMIT orders (asks), sorted by price ascending (lowest first)
        List<FuturesOrder> sellOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusAndType(
                        symbol,
                        FuturesOrder.OrderSide.SELL,
                        FuturesOrder.OrderStatus.PENDING,
                        FuturesOrder.OrderType.LIMIT,
                        PageRequest.of(0, limit, Sort.by("price").ascending()));

        // Aggregate orders by price level
        List<List<String>> bids = aggregateOrdersByPrice(buyOrders);
        List<List<String>> asks = aggregateOrdersByPrice(sellOrders);

        return Map.of(
                "symbol", symbol,
                "lastUpdateId", System.currentTimeMillis(),
                "bids", bids,
                "asks", asks);
    }

    /**
     * P2P Matching Engine
     * Scans the order book for the given symbol and executes trades if crosses
     * exist.
     */
    @Transactional
    public void matchOrders(String symbol) {
        // 1. Fetch Pending Orders
        // Buy Orders: Highest Price First (Best Bid)
        List<FuturesOrder> buyOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusOrderByPriceDescCreatedAtAsc(
                        symbol, FuturesOrder.OrderSide.BUY, FuturesOrder.OrderStatus.PENDING);

        // Sell Orders: Lowest Price First (Best Ask)
        List<FuturesOrder> sellOrders = futuresOrderRepository
                .findBySymbolAndSideAndStatusOrderByPriceAscCreatedAtAsc(
                        symbol, FuturesOrder.OrderSide.SELL, FuturesOrder.OrderStatus.PENDING);

        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            return; // No liquidity to match
        }

        // 2. Matching Loop
        int buyIndex = 0;
        int sellIndex = 0;

        while (buyIndex < buyOrders.size() && sellIndex < sellOrders.size()) {
            FuturesOrder bestBuy = buyOrders.get(buyIndex);
            FuturesOrder bestSell = sellOrders.get(sellIndex);

            // Check for Cross: Best Buy Price >= Best Sell Price
            if (bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0) {
                // MATCH FOUND!

                // Determine Trade Price (Maker's Price)
                // If Buy Order came first, it's the Maker -> Price = Buy Price
                // If Sell Order came first, it's the Maker -> Price = Sell Price
                BigDecimal tradePrice;
                if (bestBuy.getCreatedAt().isBefore(bestSell.getCreatedAt())) {
                    tradePrice = bestBuy.getPrice();
                } else {
                    tradePrice = bestSell.getPrice();
                }

                // Determine Trade Quantity (Min of remaining quantities)
                BigDecimal tradeQuantity = bestBuy.getQuantity().min(bestSell.getQuantity());

                // Execute Trade
                System.out.println("⚡ P2P MATCH: Buy " + bestBuy.getId() + " vs Sell " + bestSell.getId() + " @ "
                        + tradePrice + " Qty: " + tradeQuantity);
                executeTrade(bestBuy, bestSell, tradePrice, tradeQuantity);
                if (bestBuy.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    buyIndex++;
                }
                if (bestSell.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    sellIndex++;
                }
            } else {
                // No more crosses possible (since lists are sorted)
                break;
            }
        }
    }

    /**
     * Helper method to aggregate orders by price level
     */
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
