# Hướng Dẫn Triển Khai API Hủy Lệnh Futures

## 📋 Tổng Quan

Tài liệu này hướng dẫn cách triển khai API hủy lệnh cho hệ thống Futures Trading.

---

## 🎯 Yêu Cầu

### Chức Năng
- ✅ Cho phép người dùng hủy lệnh PENDING hoặc PARTIALLY_FILLED
- ✅ Giải phóng margin đã khóa
- ✅ Cập nhật trạng thái lệnh thành CANCELLED
- ✅ Kiểm tra quyền sở hữu lệnh
- ✅ Ngăn chặn hủy lệnh đã FILLED

### Bảo Mật
- 🔒 Chỉ người tạo lệnh mới có thể hủy
- 🔒 Xác thực JWT token
- 🔒 Validate trạng thái lệnh

---

## 📝 Implementation Steps

### Step 1: Thêm Method vào Repository

**File**: `FuturesOrderRepository.java`

```java
package api.exchange.repository;

import api.exchange.models.FuturesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuturesOrderRepository extends JpaRepository<FuturesOrder, Long> {
    
    // Existing methods...
    
    /**
     * Tìm lệnh theo ID
     */
    Optional<FuturesOrder> findById(Long id);
    
    /**
     * Tìm lệnh theo ID và UID (để kiểm tra quyền sở hữu)
     */
    Optional<FuturesOrder> findByIdAndUid(Long id, String uid);
}
```

---

### Step 2: Thêm Service Method

**File**: `FuturesTradingService.java`

Thêm method sau vào class `FuturesTradingService`:

```java
/**
 * Hủy lệnh
 * @param uid User ID
 * @param orderId Order ID cần hủy
 * @throws RuntimeException nếu lệnh không tồn tại, không có quyền, hoặc không thể hủy
 */
@Transactional
public void cancelOrder(String uid, Long orderId) {
    // 1. Tìm lệnh
    FuturesOrder order = futuresOrderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    
    // 2. Kiểm tra quyền sở hữu
    if (!order.getUid().equals(uid)) {
        throw new RuntimeException("Unauthorized to cancel this order");
    }
    
    // 3. Kiểm tra trạng thái - chỉ cho phép hủy PENDING hoặc PARTIALLY_FILLED
    if (order.getStatus() != FuturesOrder.OrderStatus.PENDING &&
        order.getStatus() != FuturesOrder.OrderStatus.PARTIALLY_FILLED) {
        throw new RuntimeException("Cannot cancel order in current status: " + order.getStatus());
    }
    
    // 4. Tính toán margin cần giải phóng
    BigDecimal notionalValue = order.getPrice().multiply(order.getQuantity());
    BigDecimal lockedMargin = notionalValue.divide(
        BigDecimal.valueOf(order.getLeverage()), 
        8, 
        RoundingMode.HALF_UP
    );
    
    // 5. Lấy ví và giải phóng margin
    FuturesWallet wallet = futuresWalletRepository
        .findByUidAndCurrency(uid, "USDT")
        .orElseThrow(() -> new RuntimeException("Wallet not found"));
    
    // Giảm locked balance
    BigDecimal newLockedBalance = wallet.getLockedBalance().subtract(lockedMargin);
    
    // Đảm bảo không âm
    if (newLockedBalance.compareTo(BigDecimal.ZERO) < 0) {
        newLockedBalance = BigDecimal.ZERO;
    }
    
    wallet.setLockedBalance(newLockedBalance);
    futuresWalletRepository.save(wallet);
    
    // 6. Cập nhật trạng thái lệnh
    order.setStatus(FuturesOrder.OrderStatus.CANCELLED);
    futuresOrderRepository.save(order);
    
    // 7. Log
    System.out.println(String.format(
        "❌ CANCELLED Order: ID=%d, Symbol=%s, User=%s, Margin Released=%.2f USDT",
        orderId, order.getSymbol(), uid, lockedMargin
    ));
}
```

---

### Step 3: Thêm Controller Endpoint

**File**: `FuturesController.java`

Thêm method sau vào class `FuturesController`:

```java
/**
 * Hủy lệnh
 */
@DeleteMapping("/order/{orderId}")
public ResponseEntity<?> cancelOrder(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long orderId) {
    try {
        String uid = getUidFromPrincipal(userDetails);
        futuresTradingService.cancelOrder(uid, orderId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Order cancelled successfully",
            "orderId", orderId,
            "status", "CANCELLED",
            "cancelledAt", LocalDateTime.now().toString()
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

---

### Step 4: Testing

#### Test Case 1: Hủy Lệnh Thành Công

```bash
# 1. Đặt lệnh LIMIT
curl -X POST http://localhost:8000/api/v1/futures/order \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "side": "BUY",
    "positionSide": "LONG",
    "type": "LIMIT",
    "price": 40000.00,
    "quantity": 0.1,
    "leverage": 10
  }'

# Response: { "id": 12345, "status": "PENDING", ... }

# 2. Hủy lệnh
curl -X DELETE http://localhost:8000/api/v1/futures/order/12345 \
  -H "Authorization: Bearer <token>"

# Expected Response:
{
  "message": "Order cancelled successfully",
  "orderId": 12345,
  "status": "CANCELLED",
  "cancelledAt": "2025-12-01T10:35:00"
}
```

#### Test Case 2: Hủy Lệnh Đã FILLED (Thất Bại)

```bash
# Giả sử lệnh 12344 đã FILLED
curl -X DELETE http://localhost:8000/api/v1/futures/order/12344 \
  -H "Authorization: Bearer <token>"

# Expected Response (400 Bad Request):
{
  "message": "Cannot cancel order in current status: FILLED"
}
```

#### Test Case 3: Hủy Lệnh Của User Khác (Thất Bại)

```bash
curl -X DELETE http://localhost:8000/api/v1/futures/order/12345 \
  -H "Authorization: Bearer <other_user_token>"

# Expected Response (400 Bad Request):
{
  "message": "Unauthorized to cancel this order"
}
```

#### Test Case 4: Hủy Lệnh Không Tồn Tại (Thất Bại)

```bash
curl -X DELETE http://localhost:8000/api/v1/futures/order/99999 \
  -H "Authorization: Bearer <token>"

# Expected Response (400 Bad Request):
{
  "message": "Order not found"
}
```

---

## 🔍 Verification Checklist

Sau khi triển khai, kiểm tra các điểm sau:

### Database
- [ ] Trạng thái lệnh được cập nhật thành `CANCELLED`
- [ ] `locked_balance` trong `futures_wallets` giảm đúng số margin
- [ ] `updated_at` của lệnh được cập nhật

### Business Logic
- [ ] Chỉ lệnh PENDING hoặc PARTIALLY_FILLED có thể bị hủy
- [ ] Lệnh FILLED không thể bị hủy
- [ ] Lệnh CANCELLED không thể bị hủy lại
- [ ] Chỉ chủ sở hữu mới có thể hủy lệnh

### Security
- [ ] JWT token được validate
- [ ] User ID được extract từ token
- [ ] Kiểm tra quyền sở hữu lệnh

### Error Handling
- [ ] Xử lý lệnh không tồn tại
- [ ] Xử lý unauthorized access
- [ ] Xử lý trạng thái không hợp lệ
- [ ] Xử lý wallet không tồn tại

---

## 📊 Database Schema

Đảm bảo schema đúng:

```sql
-- futures_orders table
CREATE TABLE futures_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(255) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    position_side VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    price DECIMAL(24, 8),
    quantity DECIMAL(24, 8) NOT NULL,
    leverage INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uid_status (uid, status),
    INDEX idx_symbol_status (symbol, status)
);

-- futures_wallets table
CREATE TABLE futures_wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(255) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(24, 8) NOT NULL DEFAULT 0,
    locked_balance DECIMAL(24, 8) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_uid_currency (uid, currency)
);
```

---

## 🚀 Advanced Features (Optional)

### 1. Batch Cancel

Hủy nhiều lệnh cùng lúc:

```java
@DeleteMapping("/orders")
public ResponseEntity<?> cancelOrders(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody Map<String, Object> request) {
    try {
        String uid = getUidFromPrincipal(userDetails);
        List<Long> orderIds = (List<Long>) request.get("orderIds");
        
        List<Long> cancelled = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        
        for (Long orderId : orderIds) {
            try {
                futuresTradingService.cancelOrder(uid, orderId);
                cancelled.add(orderId);
            } catch (Exception e) {
                errors.add(Map.of(
                    "orderId", orderId,
                    "error", e.getMessage()
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "cancelled", cancelled,
            "errors", errors
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

### 2. Cancel All Orders

Hủy tất cả lệnh của một symbol:

```java
@DeleteMapping("/orders/all")
public ResponseEntity<?> cancelAllOrders(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(required = false) String symbol) {
    try {
        String uid = getUidFromPrincipal(userDetails);
        
        List<FuturesOrder> orders;
        if (symbol != null) {
            orders = futuresOrderRepository.findByUidAndSymbolAndStatus(
                uid, symbol, FuturesOrder.OrderStatus.PENDING
            );
        } else {
            orders = futuresOrderRepository.findByUidAndStatus(
                uid, FuturesOrder.OrderStatus.PENDING
            );
        }
        
        int cancelledCount = 0;
        for (FuturesOrder order : orders) {
            try {
                futuresTradingService.cancelOrder(uid, order.getId());
                cancelledCount++;
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Failed to cancel order " + order.getId() + ": " + e.getMessage());
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Orders cancelled",
            "count", cancelledCount
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

---

## 📝 Code Summary

### Files to Modify

1. **FuturesOrderRepository.java**
   - Thêm method `findByIdAndUid()`

2. **FuturesTradingService.java**
   - Thêm method `cancelOrder(String uid, Long orderId)`

3. **FuturesController.java**
   - Thêm endpoint `DELETE /order/{orderId}`

### Total Lines of Code
- Repository: ~5 lines
- Service: ~60 lines
- Controller: ~20 lines
- **Total**: ~85 lines

---

## 🎓 Best Practices

1. **Transaction Management**
   - Sử dụng `@Transactional` để đảm bảo atomicity
   - Rollback nếu có lỗi

2. **Error Handling**
   - Throw exception rõ ràng
   - Return meaningful error messages

3. **Logging**
   - Log mọi thao tác hủy lệnh
   - Include user ID, order ID, và margin released

4. **Validation**
   - Validate ownership
   - Validate order status
   - Validate wallet existence

5. **Performance**
   - Sử dụng index trên `uid` và `status`
   - Tránh N+1 query problem

---

## 📚 Related Documentation

- [Futures Order API](./FUTURES_ORDER_API.md)
- [Futures API Endpoints](./FUTURES_API_ENDPOINTS.md)
- [Futures API README](./FUTURES_API_README.md)

---

**Version**: 1.0  
**Created**: 2025-12-01  
**Author**: API Exchange Development Team
