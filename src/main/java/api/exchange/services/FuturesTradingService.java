package api.exchange.services;

import api.exchange.models.*;
import api.exchange.repository.FuturesOrderRepository;
import api.exchange.repository.FuturesPositionRepository;
import api.exchange.repository.FuturesWalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

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

        // 7. Execute Market Order Immediately
        if (type == FuturesOrder.OrderType.MARKET) {
            executeOrder(order, currentPrice);
        }

        return order;
    }

    @Transactional
    public void executeOrder(FuturesOrder order, BigDecimal executionPrice) {
        // Update Order Status
        order.setStatus(FuturesOrder.OrderStatus.FILLED);
        order.setPrice(executionPrice); // Ensure price is set to actual execution price
        futuresOrderRepository.save(order);

        // Update/Create Position
        Optional<FuturesPosition> existingPosition = futuresPositionRepository.findByUidAndSymbolAndStatus(
                order.getUid(), order.getSymbol(), FuturesPosition.PositionStatus.OPEN);

        // Simplified Logic: Assuming One-Way Mode or matching Position Side
        // For this MVP, let's assume we are opening a new position or adding to
        // existing one of same side

        FuturesPosition position;
        if (existingPosition.isPresent()) {
            position = existingPosition.get();
            // Check if same side
            if (position.getSide().name().equals(order.getPositionSide().name())) {
                // Add to position
                // New Entry Price = ((OldQty * OldEntry) + (NewQty * NewEntry)) / TotalQty
                BigDecimal totalQuantity = position.getQuantity().add(order.getQuantity());
                BigDecimal totalCost = position.getQuantity().multiply(position.getEntryPrice())
                        .add(order.getQuantity().multiply(executionPrice));
                BigDecimal newEntryPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);

                position.setEntryPrice(newEntryPrice);
                position.setQuantity(totalQuantity);

                // Update Margin
                BigDecimal notionalValue = executionPrice.multiply(order.getQuantity());
                BigDecimal addedMargin = notionalValue.divide(BigDecimal.valueOf(order.getLeverage()), 8,
                        RoundingMode.HALF_UP);
                position.setMargin(position.getMargin().add(addedMargin));
            } else {
                // Reduce/Close position (Hedge mode logic or simple close)
                // For MVP, let's throw error if trying to open opposite side in same position
                // record
                // Ideally, we should handle reducing the position here.
                throw new RuntimeException("Opposite side position handling not implemented in MVP");
            }
        } else {
            // Create new position
            position = new FuturesPosition();
            position.setUid(order.getUid());
            position.setSymbol(order.getSymbol());
            position.setSide(FuturesPosition.PositionSide.valueOf(order.getPositionSide().name()));
            position.setEntryPrice(executionPrice);
            position.setQuantity(order.getQuantity());
            position.setLeverage(order.getLeverage());
            position.setStatus(FuturesPosition.PositionStatus.OPEN);

            BigDecimal notionalValue = executionPrice.multiply(order.getQuantity());
            BigDecimal margin = notionalValue.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);
            position.setMargin(margin);

            // Calculate Liquidation Price (Simplified for Long)
            // Liq Price = Entry Price * (1 - 1/Leverage + MaintenanceMarginRate)
            // Using simplified: Entry * (1 - 1/Lev)
            if (position.getSide() == FuturesPosition.PositionSide.LONG) {
                BigDecimal liqPrice = executionPrice.multiply(BigDecimal.ONE.subtract(
                        BigDecimal.ONE.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP)));
                position.setLiquidationPrice(liqPrice);
            } else {
                BigDecimal liqPrice = executionPrice.multiply(BigDecimal.ONE
                        .add(BigDecimal.ONE.divide(BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP)));
                position.setLiquidationPrice(liqPrice);
            }
        }

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
}
