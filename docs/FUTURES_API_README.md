# Tài Liệu API Futures - Tổng Hợp

## 📚 Mục Lục Tài Liệu

### 1. [Quản Lý Lệnh (Order Management)](./FUTURES_ORDER_API.md)
Tài liệu đầy đủ về API đặt lệnh, hủy lệnh và quản lý lệnh Futures.

**Nội dung chính:**
- ✅ API đặt lệnh (MARKET, LIMIT, STOP)
- ⚠️ API hủy lệnh (đề xuất - chưa triển khai)
- ⚠️ API lấy danh sách lệnh (đề xuất - chưa triển khai)
- 📊 Các loại lệnh và trạng thái
- 💰 Cách tính margin
- 📝 Ví dụ sử dụng chi tiết

**Endpoints chính:**
```
POST   /api/v1/futures/order          # Đặt lệnh
DELETE /api/v1/futures/order/{id}     # Hủy lệnh (đề xuất)
GET    /api/v1/futures/orders         # Lấy danh sách lệnh (đề xuất)
```

---

### 2. [Sổ Lệnh (Order Book)](./FUTURES_ORDERBOOK.md)
Tài liệu chi tiết về cấu trúc Order Book, cơ chế khớp lệnh và API.

**Nội dung chính:**
- 📖 Cấu trúc Order Book (Bids/Asks)
- ⚙️ Cơ chế khớp lệnh hiện tại (Scheduler-based)
- 🚀 Matching Engine lý tưởng (đề xuất)
- 🔄 WebSocket real-time updates
- 📊 Phân tích market depth
- 💹 Tính toán slippage

**Endpoints chính:**
```
GET /api/v1/futures/orderbook/{symbol}  # Lấy order book (đề xuất)
WS  /ws/futures/orderbook                # WebSocket updates (đề xuất)
```

---

### 3. [Dữ Liệu Nến (Kline/Candlestick)](./FUTURES_KLINE_API.md)
Tài liệu về API lấy dữ liệu nến cho biểu đồ giá Futures.

**Nội dung chính:**
- 📈 Lấy dữ liệu nến theo khung thời gian (1s, 1m, 1h)
- 🔄 WebSocket streaming real-time
- 📊 Cấu trúc dữ liệu OHLCV
- ⚡ Tối ưu hóa performance

**Endpoints chính:**
```
GET /api/v1/futures/klines/{symbol}  # Lấy dữ liệu nến
WS  /ws/futures/klines               # WebSocket kline updates
```

---

## 🎯 Quick Start

### Xác Thực (Authentication)

Tất cả API Futures yêu cầu JWT token:

```bash
# 1. Đăng nhập để lấy token
curl -X POST https://api.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}

# 2. Sử dụng token trong các request
curl -X GET https://api.example.com/api/v1/futures/balance \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🔥 Các API Phổ Biến

### 1. Kiểm Tra Số Dư Futures

```bash
GET /api/v1/futures/balance
```

**Response:**
```json
{
  "uid": "user123",
  "currency": "USDT",
  "balance": 10000.00,
  "lockedBalance": 450.00,
  "availableBalance": 9550.00
}
```

---

### 2. Đặt Lệnh Market

```bash
POST /api/v1/futures/order
Content-Type: application/json

{
  "symbol": "BTCUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "MARKET",
  "quantity": 0.1,
  "leverage": 10
}
```

**Response:**
```json
{
  "id": 12345,
  "symbol": "BTCUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "MARKET",
  "price": 45123.50,
  "quantity": 0.1,
  "leverage": 10,
  "status": "FILLED",
  "createdAt": "2025-12-01T10:30:00"
}
```

---

### 3. Đặt Lệnh Limit

```bash
POST /api/v1/futures/order
Content-Type: application/json

{
  "symbol": "ETHUSDT",
  "side": "BUY",
  "positionSide": "LONG",
  "type": "LIMIT",
  "price": 2400.00,
  "quantity": 0.5,
  "leverage": 20
}
```

---

### 4. Xem Vị Thế Đang Mở

```bash
GET /api/v1/futures/positions
```

**Response:**
```json
[
  {
    "id": 1,
    "uid": "user123",
    "symbol": "BTCUSDT",
    "side": "LONG",
    "entryPrice": 45000.00,
    "quantity": 0.1,
    "leverage": 10,
    "margin": 450.00,
    "liquidationPrice": 40500.00,
    "unrealizedPnl": 12.35,
    "status": "OPEN"
  }
]
```

---

### 5. Đóng Vị Thế

```bash
POST /api/v1/futures/position/close
Content-Type: application/json

{
  "symbol": "BTCUSDT"
}
```

---

### 6. Điều Chỉnh Đòn Bẩy

```bash
POST /api/v1/futures/leverage
Content-Type: application/json

{
  "symbol": "BTCUSDT",
  "leverage": 15
}
```

---

### 7. Chuyển Tiền Vào/Ra Futures Wallet

```bash
POST /api/v1/futures/transfer
Content-Type: application/json

{
  "type": "TO_FUTURES",
  "amount": 1000.00
}
```

**Types:**
- `TO_FUTURES`: Chuyển từ Spot sang Futures
- `FROM_FUTURES`: Chuyển từ Futures sang Spot

---

### 8. Lấy Danh Sách Coin Futures

```bash
GET /api/v1/futures/coins
```

**Response:**
```json
{
  "message": "success",
  "data": [
    {
      "symbol": "BTCUSDT",
      "markPrice": 45123.50,
      "indexPrice": 45120.00,
      "fundingRate": 0.0001,
      "nextFundingTime": "2025-12-01T16:00:00",
      "volume24h": 123456.78,
      "priceChange24h": 2.5
    }
  ]
}
```

---

## 📊 Cấu Trúc Dữ Liệu

### FuturesOrder

```java
{
  "id": Long,
  "uid": String,
  "symbol": String,              // VD: "BTCUSDT"
  "side": OrderSide,             // BUY, SELL
  "positionSide": PositionSide,  // LONG, SHORT
  "type": OrderType,             // MARKET, LIMIT, STOP_MARKET, STOP_LIMIT
  "price": BigDecimal,
  "quantity": BigDecimal,
  "leverage": Integer,           // 1-125
  "status": OrderStatus,         // PENDING, FILLED, CANCELLED, PARTIALLY_FILLED
  "createdAt": LocalDateTime,
  "updatedAt": LocalDateTime
}
```

### FuturesPosition

```java
{
  "id": Long,
  "uid": String,
  "symbol": String,
  "side": PositionSide,          // LONG, SHORT
  "entryPrice": BigDecimal,
  "quantity": BigDecimal,
  "leverage": Integer,
  "margin": BigDecimal,
  "liquidationPrice": BigDecimal,
  "unrealizedPnl": BigDecimal,
  "status": PositionStatus,      // OPEN, CLOSED
  "createdAt": LocalDateTime,
  "updatedAt": LocalDateTime
}
```

---

## ⚠️ Trạng Thái Triển Khai

### ✅ Đã Triển Khai

| Feature | Status | Endpoint |
|---------|--------|----------|
| Đặt lệnh MARKET | ✅ | `POST /api/v1/futures/order` |
| Đặt lệnh LIMIT | ✅ | `POST /api/v1/futures/order` |
| Xem vị thế | ✅ | `GET /api/v1/futures/positions` |
| Đóng vị thế | ✅ | `POST /api/v1/futures/position/close` |
| Điều chỉnh đòn bẩy | ✅ | `POST /api/v1/futures/leverage` |
| Chuyển tiền | ✅ | `POST /api/v1/futures/transfer` |
| Xem số dư | ✅ | `GET /api/v1/futures/balance` |
| Lấy danh sách coin | ✅ | `GET /api/v1/futures/coins` |
| Lấy dữ liệu Kline | ✅ | `GET /api/v1/futures/klines/{symbol}` |

### ⚠️ Chưa Triển Khai (Đề Xuất)

| Feature | Status | Endpoint Đề Xuất |
|---------|--------|------------------|
| Hủy lệnh | ⚠️ | `DELETE /api/v1/futures/order/{id}` |
| Lấy danh sách lệnh | ⚠️ | `GET /api/v1/futures/orders` |
| Order Book API | ⚠️ | `GET /api/v1/futures/orderbook/{symbol}` |
| WebSocket Order Book | ⚠️ | `WS /ws/futures/orderbook` |
| Matching Engine | ⚠️ | Service layer |
| Lịch sử giao dịch | ⚠️ | `GET /api/v1/futures/trades` |
| Stop Loss / Take Profit | ⚠️ | Trong `FuturesOrderRequest` |

---

## 🔧 Cơ Chế Hoạt Động

### 1. Khớp Lệnh Hiện Tại

```
┌─────────────┐
│ User đặt    │
│ lệnh LIMIT  │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ Lưu vào Database    │
│ Status: PENDING     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────────────┐
│ Scheduler (mỗi 1 giây)      │
│ - Lấy giá thị trường        │
│ - So sánh với giá đặt       │
│ - Khớp nếu điều kiện đúng   │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────┐
│ Thực hiện lệnh      │
│ - Tạo/Cập nhật vị thế│
│ - Cập nhật wallet   │
│ Status: FILLED      │
└─────────────────────┘
```

### 2. Tính Toán Margin

```
Notional Value = Price × Quantity
Initial Margin = Notional Value / Leverage
Available Balance = Balance - Locked Balance

Ví dụ:
- Price: 45,000 USDT
- Quantity: 0.1 BTC
- Leverage: 10x

Notional Value = 45,000 × 0.1 = 4,500 USDT
Required Margin = 4,500 / 10 = 450 USDT
```

### 3. Tính Toán Liquidation Price

**LONG Position:**
```
Liquidation Price = Entry Price × (1 - 1/Leverage)

Ví dụ:
Entry Price: 45,000 USDT
Leverage: 10x
Liq Price = 45,000 × (1 - 1/10) = 40,500 USDT
```

**SHORT Position:**
```
Liquidation Price = Entry Price × (1 + 1/Leverage)

Ví dụ:
Entry Price: 45,000 USDT
Leverage: 10x
Liq Price = 45,000 × (1 + 1/10) = 49,500 USDT
```

### 4. Tính Toán PnL

**LONG Position:**
```
Unrealized PnL = (Current Price - Entry Price) × Quantity

Ví dụ:
Entry: 45,000 USDT
Current: 46,000 USDT
Quantity: 0.1 BTC
PnL = (46,000 - 45,000) × 0.1 = 100 USDT
```

**SHORT Position:**
```
Unrealized PnL = (Entry Price - Current Price) × Quantity

Ví dụ:
Entry: 45,000 USDT
Current: 44,000 USDT
Quantity: 0.1 BTC
PnL = (45,000 - 44,000) × 0.1 = 100 USDT
```

---

## 🚨 Lưu Ý Quan Trọng

### Quản Lý Rủi Ro

1. **Liquidation**: Vị thế sẽ bị thanh lý khi giá chạm mức liquidation price
2. **Đòn bẩy cao**: Leverage càng cao, rủi ro thanh lý càng lớn
3. **Funding Rate**: Phí tài trợ được tính định kỳ (thường 8 giờ/lần)
4. **Margin Call**: Cần thêm margin khi vị thế gần bị thanh lý

### Giới Hạn

- **Leverage**: 1x - 125x
- **Minimum Order Size**: Phụ thuộc vào từng cặp
- **Maximum Position Size**: Phụ thuộc vào balance
- **Rate Limit**: 1200 requests/phút

### Bảo Mật

1. ✅ Luôn sử dụng HTTPS
2. ✅ Bảo mật JWT token
3. ✅ Không chia sẻ credentials
4. ✅ Sử dụng 2FA nếu có

---

## 📞 Hỗ Trợ

Nếu có thắc mắc hoặc cần hỗ trợ:

- 📧 Email: support@example.com
- 💬 Discord: [Link Discord]
- 📖 Documentation: [Link Docs]
- 🐛 Bug Report: [Link GitHub Issues]

---

## 🔄 Cập Nhật

**Version 1.0** - 2025-12-01
- ✅ Initial release
- ✅ Basic order management
- ✅ Position management
- ✅ Kline data API
- ⚠️ Order Book (đề xuất)
- ⚠️ Cancel order (đề xuất)

---

**Tác giả**: API Exchange Development Team  
**License**: Proprietary
