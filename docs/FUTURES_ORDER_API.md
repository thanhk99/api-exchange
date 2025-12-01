# Tài Liệu API Futures - Quản Lý Lệnh (Order Management)

## Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Các Loại Lệnh](#các-loại-lệnh)
3. [API Đặt Lệnh](#api-đặt-lệnh)
4. [API Hủy Lệnh](#api-hủy-lệnh)
5. [API Lấy Danh Sách Lệnh](#api-lấy-danh-sách-lệnh)
6. [API Sổ Lệnh (Order Book)](#api-sổ-lệnh-order-book)
7. [Các Trạng Thái Lệnh](#các-trạng-thái-lệnh)
8. [Ví Dụ Sử Dụng](#ví-dụ-sử-dụng)

---

## Tổng Quan

Hệ thống Futures Trading hỗ trợ các loại lệnh giao dịch phái sinh với đòn bẩy (leverage) từ 1x đến 125x. Người dùng có thể mở vị thế LONG hoặc SHORT và quản lý các lệnh của mình thông qua các API sau.

**Base URL**: `/api/v1/futures`

**Authentication**: Tất cả các endpoint yêu cầu JWT token trong header:
```
Authorization: Bearer <your_jwt_token>
```

---

## Các Loại Lệnh

### 1. Order Side (Hướng Lệnh)
- **BUY**: Mua (mở hoặc tăng vị thế)
- **SELL**: Bán (mở hoặc giảm vị thế)

### 2. Position Side (Hướng Vị Thế)
- **LONG**: Vị thế mua (kỳ vọng giá tăng)
- **SHORT**: Vị thế bán (kỳ vọng giá giảm)

### 3. Order Type (Loại Lệnh)
- **MARKET**: Lệnh thị trường - khớp ngay lập tức theo giá hiện tại
- **LIMIT**: Lệnh giới hạn - chờ khớp khi giá đạt mức đặt trước
- **STOP_MARKET**: Lệnh dừng lỗ thị trường
- **STOP_LIMIT**: Lệnh dừng lỗ giới hạn

### 4. Order Status (Trạng Thái Lệnh)
- **PENDING**: Đang chờ khớp
- **FILLED**: Đã khớp hoàn toàn
- **PARTIALLY_FILLED**: Khớp một phần
- **CANCELLED**: Đã hủy

---

## API Đặt Lệnh

### Endpoint
```
POST /api/v1/futures/order
```

### Request Headers
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### Request Body
```json
{
  "symbol": "BTCUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "LIMIT",
  "price": 45000.50,
  "quantity": 0.1,
  "leverage": 10
}
```

### Request Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `symbol` | String | ✅ | Cặp giao dịch (VD: BTCUSDT, ETHUSDT) |
| `side` | String | ✅ | Hướng lệnh: `BUY` hoặc `SELL` |
| `positionSide` | String | ✅ | Hướng vị thế: `LONG` hoặc `SHORT` |
| `type` | String | ✅ | Loại lệnh: `MARKET`, `LIMIT`, `STOP_MARKET`, `STOP_LIMIT` |
| `price` | BigDecimal | ⚠️ | Giá đặt lệnh (bắt buộc với LIMIT, không cần với MARKET) |
| `quantity` | BigDecimal | ✅ | Số lượng (đơn vị: coin) |
| `leverage` | Integer | ✅ | Đòn bẩy (1-125) |

### Response Success (200 OK)
```json
{
  "id": 12345,
  "uid": "user123",
  "symbol": "BTCUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "LIMIT",
  "price": 45000.50,
  "quantity": 0.1,
  "leverage": 10,
  "status": "PENDING",
  "createdAt": "2025-12-01T10:30:00",
  "updatedAt": "2025-12-01T10:30:00"
}
```

### Response Error (400 Bad Request)
```json
{
  "message": "Insufficient margin"
}
```

### Các Lỗi Thường Gặp

| Mã lỗi | Thông báo | Nguyên nhân |
|--------|-----------|-------------|
| 400 | `Quantity must be positive` | Số lượng <= 0 |
| 400 | `Invalid leverage` | Đòn bẩy không hợp lệ (< 1 hoặc > 125) |
| 400 | `Price unavailable for symbol` | Không lấy được giá cho cặp giao dịch |
| 400 | `Insufficient margin` | Số dư không đủ để mở lệnh |
| 404 | `Futures wallet not found` | Chưa có ví Futures |

### Cách Tính Margin Yêu Cầu

```
Initial Margin = (Price × Quantity) / Leverage
```

**Ví dụ:**
- Price: 45,000 USDT
- Quantity: 0.1 BTC
- Leverage: 10x

```
Notional Value = 45,000 × 0.1 = 4,500 USDT
Required Margin = 4,500 / 10 = 450 USDT
```

---

## API Hủy Lệnh

> ⚠️ **LƯU Ý**: API hủy lệnh hiện chưa được triển khai trong hệ thống. Dưới đây là thiết kế đề xuất.

### Endpoint (Đề Xuất)
```
DELETE /api/v1/futures/order/{orderId}
```

### Request Headers
```
Authorization: Bearer <jwt_token>
```

### Path Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `orderId` | Long | ID của lệnh cần hủy |

### Response Success (200 OK)
```json
{
  "message": "Order cancelled successfully",
  "orderId": 12345,
  "status": "CANCELLED",
  "cancelledAt": "2025-12-01T10:35:00"
}
```

### Response Error (400 Bad Request)
```json
{
  "message": "Cannot cancel order in current status: FILLED"
}
```

### Quy Tắc Hủy Lệnh

1. ✅ **Có thể hủy**: Lệnh có trạng thái `PENDING` hoặc `PARTIALLY_FILLED`
2. ❌ **Không thể hủy**: Lệnh có trạng thái `FILLED` hoặc `CANCELLED`
3. 🔒 **Phân quyền**: Chỉ người tạo lệnh mới có thể hủy
4. 💰 **Hoàn margin**: Margin bị khóa sẽ được giải phóng khi hủy lệnh

### Implementation Code (Đề Xuất)

**Controller:**
```java
@DeleteMapping("/order/{orderId}")
public ResponseEntity<?> cancelOrder(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long orderId) {
    try {
        String uid = getUidFromPrincipal(userDetails);
        futuresTradingService.cancelOrder(uid, orderId);
        return ResponseEntity.ok(Map.of(
            "message", "Order cancelled successfully",
            "orderId", orderId
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

**Service:**
```java
@Transactional
public void cancelOrder(String uid, Long orderId) {
    // 1. Tìm lệnh
    FuturesOrder order = futuresOrderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    
    // 2. Kiểm tra quyền sở hữu
    if (!order.getUid().equals(uid)) {
        throw new RuntimeException("Unauthorized to cancel this order");
    }
    
    // 3. Kiểm tra trạng thái
    if (order.getStatus() != FuturesOrder.OrderStatus.PENDING &&
        order.getStatus() != FuturesOrder.OrderStatus.PARTIALLY_FILLED) {
        throw new RuntimeException("Cannot cancel order in current status: " 
            + order.getStatus());
    }
    
    // 4. Giải phóng margin
    FuturesWallet wallet = futuresWalletRepository
        .findByUidAndCurrency(uid, "USDT")
        .orElseThrow(() -> new RuntimeException("Wallet not found"));
    
    BigDecimal notionalValue = order.getPrice().multiply(order.getQuantity());
    BigDecimal lockedMargin = notionalValue.divide(
        BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);
    
    wallet.setLockedBalance(wallet.getLockedBalance().subtract(lockedMargin));
    futuresWalletRepository.save(wallet);
    
    // 5. Cập nhật trạng thái lệnh
    order.setStatus(FuturesOrder.OrderStatus.CANCELLED);
    futuresOrderRepository.save(order);
}
```

---

## API Lấy Danh Sách Lệnh

> ⚠️ **LƯU Ý**: API này hiện chưa được triển khai. Dưới đây là thiết kế đề xuất.

### Endpoint (Đề Xuất)
```
GET /api/v1/futures/orders
```

### Request Headers
```
Authorization: Bearer <jwt_token>
```

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `symbol` | String | ❌ | Lọc theo cặp giao dịch |
| `status` | String | ❌ | Lọc theo trạng thái: `PENDING`, `FILLED`, `CANCELLED` |
| `limit` | Integer | ❌ | Số lượng kết quả (mặc định: 50, tối đa: 500) |
| `offset` | Integer | ❌ | Vị trí bắt đầu (mặc định: 0) |

### Response Success (200 OK)
```json
{
  "message": "success",
  "data": [
    {
      "id": 12345,
      "uid": "user123",
      "symbol": "BTCUSDT",
      "side": "BUY",
      "positionSide": "LONG",
      "type": "LIMIT",
      "price": 45000.50,
      "quantity": 0.1,
      "leverage": 10,
      "status": "PENDING",
      "createdAt": "2025-12-01T10:30:00",
      "updatedAt": "2025-12-01T10:30:00"
    },
    {
      "id": 12344,
      "uid": "user123",
      "symbol": "ETHUSDT",
      "side": "SELL",
      "positionSide": "SHORT",
      "type": "MARKET",
      "price": 2500.00,
      "quantity": 1.5,
      "leverage": 20,
      "status": "FILLED",
      "createdAt": "2025-12-01T09:15:00",
      "updatedAt": "2025-12-01T09:15:01"
    }
  ],
  "total": 2
}
```

### Implementation Code (Đề Xuất)

**Controller:**
```java
@GetMapping("/orders")
public ResponseEntity<?> getOrders(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(required = false) String symbol,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset) {
    try {
        String uid = getUidFromPrincipal(userDetails);
        List<FuturesOrder> orders = futuresOrderService.getOrders(
            uid, symbol, status, limit, offset);
        return ResponseEntity.ok(Map.of(
            "message", "success",
            "data", orders,
            "total", orders.size()
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

**Repository:**
```java
// Trong FuturesOrderRepository.java
List<FuturesOrder> findByUidOrderByCreatedAtDesc(String uid, Pageable pageable);

List<FuturesOrder> findByUidAndSymbolOrderByCreatedAtDesc(
    String uid, String symbol, Pageable pageable);

List<FuturesOrder> findByUidAndStatusOrderByCreatedAtDesc(
    String uid, FuturesOrder.OrderStatus status, Pageable pageable);

List<FuturesOrder> findByUidAndSymbolAndStatusOrderByCreatedAtDesc(
    String uid, String symbol, FuturesOrder.OrderStatus status, Pageable pageable);
```

---

## API Sổ Lệnh (Order Book)

> ⚠️ **LƯU Ý**: Hệ thống hiện tại chưa có Order Book thực sự. Các lệnh LIMIT được khớp tự động bởi scheduler dựa trên giá thị trường.

### Cơ Chế Khớp Lệnh Hiện Tại

Hệ thống sử dụng **FuturesOrderService** với scheduler chạy mỗi giây để khớp lệnh LIMIT:

```java
@Scheduled(fixedRate = 1000) // Chạy mỗi 1 giây
@Transactional
public void matchLimitOrders() {
    List<FuturesOrder> pendingOrders = futuresOrderRepository.findAll();
    
    for (FuturesOrder order : pendingOrders) {
        if (order.getStatus() != OrderStatus.PENDING) continue;
        if (order.getType() == OrderType.MARKET) continue;
        
        BigDecimal currentPrice = coinDataService.getCurrentPrice(order.getSymbol());
        
        boolean shouldExecute = false;
        
        if (order.getSide() == OrderSide.BUY) {
            // Buy Limit: Khớp khi giá thị trường <= giá đặt
            if (currentPrice.compareTo(order.getPrice()) <= 0) {
                shouldExecute = true;
            }
        } else {
            // Sell Limit: Khớp khi giá thị trường >= giá đặt
            if (currentPrice.compareTo(order.getPrice()) >= 0) {
                shouldExecute = true;
            }
        }
        
        if (shouldExecute) {
            futuresTradingService.executeOrder(order, currentPrice);
        }
    }
}
```

### Endpoint Order Book (Đề Xuất)
```
GET /api/v1/futures/orderbook/{symbol}
```

### Query Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `symbol` | String | Cặp giao dịch (VD: BTCUSDT) |
| `limit` | Integer | Số lượng mức giá mỗi bên (mặc định: 20) |

### Response Success (200 OK)
```json
{
  "symbol": "BTCUSDT",
  "lastUpdateId": 1234567890,
  "bids": [
    ["44950.00", "0.5"],
    ["44940.00", "1.2"],
    ["44930.00", "0.8"]
  ],
  "asks": [
    ["45000.00", "0.3"],
    ["45010.00", "0.7"],
    ["45020.00", "1.5"]
  ]
}
```

**Giải thích:**
- `bids`: Lệnh mua (giá cao nhất ở trên)
- `asks`: Lệnh bán (giá thấp nhất ở trên)
- Mỗi mức giá: `[price, quantity]`

### Implementation Code (Đề Xuất)

**Controller:**
```java
@GetMapping("/orderbook/{symbol}")
public ResponseEntity<?> getOrderBook(
        @PathVariable String symbol,
        @RequestParam(defaultValue = "20") int limit) {
    try {
        var orderBook = futuresOrderService.getOrderBook(symbol, limit);
        return ResponseEntity.ok(orderBook);
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", e.getMessage()));
    }
}
```

**Service:**
```java
public Map<String, Object> getOrderBook(String symbol, int limit) {
    // Lấy lệnh BUY LIMIT đang chờ, sắp xếp giá giảm dần
    List<FuturesOrder> buyOrders = futuresOrderRepository
        .findBySymbolAndSideAndStatusAndType(
            symbol, 
            FuturesOrder.OrderSide.BUY,
            FuturesOrder.OrderStatus.PENDING,
            FuturesOrder.OrderType.LIMIT,
            PageRequest.of(0, limit, Sort.by("price").descending())
        );
    
    // Lấy lệnh SELL LIMIT đang chờ, sắp xếp giá tăng dần
    List<FuturesOrder> sellOrders = futuresOrderRepository
        .findBySymbolAndSideAndStatusAndType(
            symbol,
            FuturesOrder.OrderSide.SELL,
            FuturesOrder.OrderStatus.PENDING,
            FuturesOrder.OrderType.LIMIT,
            PageRequest.of(0, limit, Sort.by("price").ascending())
        );
    
    // Nhóm theo giá và tính tổng quantity
    List<List<String>> bids = buyOrders.stream()
        .collect(Collectors.groupingBy(
            FuturesOrder::getPrice,
            Collectors.reducing(BigDecimal.ZERO, 
                FuturesOrder::getQuantity, BigDecimal::add)
        ))
        .entrySet().stream()
        .map(e -> Arrays.asList(
            e.getKey().toString(), 
            e.getValue().toString()
        ))
        .collect(Collectors.toList());
    
    List<List<String>> asks = sellOrders.stream()
        .collect(Collectors.groupingBy(
            FuturesOrder::getPrice,
            Collectors.reducing(BigDecimal.ZERO, 
                FuturesOrder::getQuantity, BigDecimal::add)
        ))
        .entrySet().stream()
        .map(e -> Arrays.asList(
            e.getKey().toString(), 
            e.getValue().toString()
        ))
        .collect(Collectors.toList());
    
    return Map.of(
        "symbol", symbol,
        "lastUpdateId", System.currentTimeMillis(),
        "bids", bids,
        "asks", asks
    );
}
```

---

## Các Trạng Thái Lệnh

### Vòng Đời Của Một Lệnh

```mermaid
graph LR
    A[Tạo Lệnh] --> B{Loại Lệnh?}
    B -->|MARKET| C[FILLED]
    B -->|LIMIT| D[PENDING]
    D --> E{Khớp Lệnh?}
    E -->|Có| C
    E -->|Hủy| F[CANCELLED]
    D --> G{Khớp Một Phần?}
    G -->|Có| H[PARTIALLY_FILLED]
    H --> E
```

### Chi Tiết Trạng Thái

| Trạng thái | Mô tả | Hành động có thể |
|------------|-------|------------------|
| **PENDING** | Lệnh đang chờ khớp | ✅ Hủy lệnh |
| **PARTIALLY_FILLED** | Lệnh khớp một phần | ✅ Hủy phần còn lại |
| **FILLED** | Lệnh đã khớp hoàn toàn | ❌ Không thể hủy |
| **CANCELLED** | Lệnh đã bị hủy | ❌ Không thể thao tác |

---

## Ví Dụ Sử Dụng

### 1. Mở Vị Thế LONG với Lệnh MARKET

**Kịch bản**: Mua 0.1 BTC với đòn bẩy 10x tại giá thị trường

```bash
curl -X POST https://api.example.com/api/v1/futures/order \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "side": "BUY",
    "positionSide": "LONG",
    "type": "MARKET",
    "quantity": 0.1,
    "leverage": 10
  }'
```

**Response:**
```json
{
  "id": 12345,
  "uid": "user123",
  "symbol": "BTCUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "MARKET",
  "price": 45123.50,
  "quantity": 0.1,
  "leverage": 10,
  "status": "FILLED",
  "createdAt": "2025-12-01T10:30:00",
  "updatedAt": "2025-12-01T10:30:00"
}
```

### 2. Đặt Lệnh LIMIT Mua

**Kịch bản**: Đặt lệnh mua 0.5 ETH tại giá 2,400 USDT với đòn bẩy 20x

```bash
curl -X POST https://api.example.com/api/v1/futures/order \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ETHUSDT",
    "side": "BUY",
    "positionSide": "LONG",
    "type": "LIMIT",
    "price": 2400.00,
    "quantity": 0.5,
    "leverage": 20
  }'
```

**Response:**
```json
{
  "id": 12346,
  "uid": "user123",
  "symbol": "ETHUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "LIMIT",
  "price": 2400.00,
  "quantity": 0.5,
  "leverage": 20,
  "status": "PENDING",
  "createdAt": "2025-12-01T10:35:00",
  "updatedAt": "2025-12-01T10:35:00"
}
```

### 3. Mở Vị Thế SHORT

**Kịch bản**: Bán khống 1 ETH với đòn bẩy 15x

```bash
curl -X POST https://api.example.com/api/v1/futures/order \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ETHUSDT",
    "side": "SELL",
    "positionSide": "SHORT",
    "type": "MARKET",
    "quantity": 1.0,
    "leverage": 15
  }'
```

### 4. Hủy Lệnh (Đề Xuất)

```bash
curl -X DELETE https://api.example.com/api/v1/futures/order/12346 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
{
  "message": "Order cancelled successfully",
  "orderId": 12346,
  "status": "CANCELLED",
  "cancelledAt": "2025-12-01T10:40:00"
}
```

### 5. Lấy Danh Sách Lệnh Đang Chờ (Đề Xuất)

```bash
curl -X GET "https://api.example.com/api/v1/futures/orders?status=PENDING&limit=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 6. Xem Order Book (Đề Xuất)

```bash
curl -X GET "https://api.example.com/api/v1/futures/orderbook/BTCUSDT?limit=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Lưu Ý Quan Trọng

### ⚠️ Quản Lý Rủi Ro

1. **Margin Call**: Khi giá chạm mức thanh lý (liquidation price), vị thế sẽ bị đóng tự động
2. **Đòn Bẩy Cao**: Đòn bẩy càng cao, rủi ro thanh lý càng lớn
3. **Funding Rate**: Phí tài trợ được tính định kỳ cho các vị thế mở

### 🔒 Bảo Mật

1. Luôn giữ JWT token an toàn
2. Không chia sẻ API credentials
3. Sử dụng HTTPS cho tất cả các request

### 📊 Giới Hạn

- **Leverage tối đa**: 125x
- **Số lượng tối thiểu**: Phụ thuộc vào từng cặp giao dịch
- **Rate Limit**: 1200 requests/phút

---

## Tài Liệu Liên Quan

- [Futures Kline API](./FUTURES_KLINE_API.md)
- [Futures Position Management](./FUTURES_POSITION_API.md) (Chưa có)
- [Futures Wallet API](./FUTURES_WALLET_API.md) (Chưa có)

---

**Phiên bản**: 1.0  
**Cập nhật lần cuối**: 2025-12-01  
**Tác giả**: API Exchange Development Team
