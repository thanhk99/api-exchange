# API Endpoints - Futures Trading

## 📋 Danh Sách Đầy Đủ Các Endpoint

### Base URL
```
http://localhost:8000/api/v1/futures
```

---

## 🔐 Authentication

Tất cả endpoint (trừ public endpoints) yêu cầu JWT token:

```
Authorization: Bearer <your_jwt_token>
```

---

## 💰 Wallet Management

### 1. Lấy Số Dư Futures
```http
GET /api/v1/futures/balance
```

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "uid": "user123",
  "currency": "USDT",
  "balance": 10000.00,
  "lockedBalance": 450.00,
  "availableBalance": 9550.00,
  "createdAt": "2025-11-01T10:00:00",
  "updatedAt": "2025-12-01T10:30:00"
}
```

---

### 2. Chuyển Tiền Vào/Ra Futures
```http
POST /api/v1/futures/transfer
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "type": "TO_FUTURES",
  "amount": 1000.00
}
```

**Parameters:**
- `type`: `"TO_FUTURES"` hoặc `"FROM_FUTURES"`
- `amount`: Số tiền cần chuyển (BigDecimal)

**Response:**
```json
{
  "message": "Transfer successful"
}
```

**Error Response:**
```json
{
  "message": "Insufficient balance"
}
```

---

## 📝 Order Management

### 3. Đặt Lệnh
```http
POST /api/v1/futures/order
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
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

**Parameters:**

| Field | Type | Required | Values | Description |
|-------|------|----------|--------|-------------|
| `symbol` | String | ✅ | BTCUSDT, ETHUSDT, etc. | Cặp giao dịch |
| `side` | String | ✅ | BUY, SELL | Hướng lệnh |
| `positionSide` | String | ✅ | LONG, SHORT | Hướng vị thế |
| `type` | String | ✅ | MARKET, LIMIT | Loại lệnh |
| `price` | BigDecimal | ⚠️ | > 0 | Giá (bắt buộc với LIMIT) |
| `quantity` | BigDecimal | ✅ | > 0 | Số lượng |
| `leverage` | Integer | ✅ | 1-125 | Đòn bẩy |

**Response:**
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

**Error Responses:**
```json
{
  "message": "Quantity must be positive"
}
```
```json
{
  "message": "Invalid leverage"
}
```
```json
{
  "message": "Insufficient margin"
}
```

---

### 4. Hủy Lệnh ⚠️ (Chưa triển khai)
```http
DELETE /api/v1/futures/order/{orderId}
```

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `orderId`: ID của lệnh cần hủy

**Response:**
```json
{
  "message": "Order cancelled successfully",
  "orderId": 12345,
  "status": "CANCELLED",
  "cancelledAt": "2025-12-01T10:35:00"
}
```

**Error Response:**
```json
{
  "message": "Cannot cancel order in current status: FILLED"
}
```

---

### 5. Lấy Danh Sách Lệnh ⚠️ (Chưa triển khai)
```http
GET /api/v1/futures/orders
```

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `symbol` | String | ❌ | Lọc theo cặp giao dịch |
| `status` | String | ❌ | PENDING, FILLED, CANCELLED |
| `limit` | Integer | ❌ | Số lượng (mặc định: 50, max: 500) |
| `offset` | Integer | ❌ | Vị trí bắt đầu (mặc định: 0) |

**Example:**
```
GET /api/v1/futures/orders?symbol=BTCUSDT&status=PENDING&limit=10
```

**Response:**
```json
{
  "message": "success",
  "data": [
    {
      "id": 12345,
      "symbol": "BTCUSDT",
      "side": "BUY",
      "positionSide": "LONG",
      "type": "LIMIT",
      "price": 45000.50,
      "quantity": 0.1,
      "leverage": 10,
      "status": "PENDING",
      "createdAt": "2025-12-01T10:30:00"
    }
  ],
  "total": 1
}
```

---

## 📊 Position Management

### 6. Lấy Danh Sách Vị Thế
```http
GET /api/v1/futures/positions
```

**Headers:**
```
Authorization: Bearer <token>
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
    "status": "OPEN",
    "createdAt": "2025-12-01T10:00:00",
    "updatedAt": "2025-12-01T10:30:00"
  }
]
```

---

### 7. Đóng Vị Thế
```http
POST /api/v1/futures/position/close
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "symbol": "BTCUSDT"
}
```

**Response:**
```json
{
  "message": "Position closed successfully"
}
```

**Error Response:**
```json
{
  "message": "Position not found"
}
```

---

### 8. Điều Chỉnh Đòn Bẩy
```http
POST /api/v1/futures/leverage
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "symbol": "BTCUSDT",
  "leverage": 15
}
```

**Parameters:**
- `symbol`: Cặp giao dịch
- `leverage`: Đòn bẩy mới (1-125)

**Response:**
```json
{
  "message": "Leverage adjusted successfully"
}
```

**Error Responses:**
```json
{
  "message": "Invalid leverage"
}
```
```json
{
  "message": "Insufficient balance to increase margin for lower leverage"
}
```

---

## 📈 Market Data

### 9. Lấy Danh Sách Coin Futures
```http
GET /api/v1/futures/coins
```

**Headers:** Không yêu cầu authentication (Public endpoint)

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
      "priceChange24h": 2.5,
      "high24h": 46000.00,
      "low24h": 44000.00
    },
    {
      "symbol": "ETHUSDT",
      "markPrice": 2500.00,
      "indexPrice": 2498.50,
      "fundingRate": 0.00015,
      "nextFundingTime": "2025-12-01T16:00:00",
      "volume24h": 45678.90,
      "priceChange24h": 1.8
    }
  ]
}
```

---

### 10. Lấy Dữ Liệu Kline (Nến)
```http
POST /api/v1/futuresKline/symbol
```

**Headers:**
```
Content-Type: application/json
```

**Query Parameters:**
- `limit`: Số lượng nến (mặc định: 72, max: 500)

**Request Body:**
```json
{
  "symbol": "BTCUSDT",
  "interval": "1m"
}
```

**Supported Intervals:**
- `1m`, `5m`, `15m`, `30m`
- `1h`, `2h`, `4h`, `6h`, `12h`
- `1d`, `1w`

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "interval": "1m",
  "count": 288,
  "limit": 288,
  "success": true,
  "message": "Dữ liệu futures kline được lấy thành công",
  "data": [
    {
      "symbol": "BTCUSDT",
      "openPrice": 45100.00,
      "closePrice": 45120.00,
      "highPrice": 45150.00,
      "lowPrice": 45080.00,
      "volume": 125.45,
      "startTime": 1733043600000,
      "closeTime": 1733043659999,
      "interval": "1m",
      "isClosed": true
    }
  ]
}
```

---

### 11. Lấy Order Book ⚠️ (Chưa triển khai)
```http
GET /api/v1/futures/orderbook/{symbol}
```

**Headers:** Không yêu cầu authentication (Public endpoint)

**Path Parameters:**
- `symbol`: Cặp giao dịch (VD: BTCUSDT)

**Query Parameters:**
- `limit`: Số mức giá mỗi bên (mặc định: 20, max: 100)

**Example:**
```
GET /api/v1/futures/orderbook/BTCUSDT?limit=20
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "lastUpdateId": 1701432000000,
  "bids": [
    ["44950.00", "0.500"],
    ["44940.00", "1.200"],
    ["44930.00", "0.800"]
  ],
  "asks": [
    ["45000.00", "0.300"],
    ["45010.00", "0.700"],
    ["45020.00", "1.500"]
  ],
  "spread": {
    "absolute": 50.00,
    "percentage": 0.11
  },
  "depth": {
    "bidVolume": 2.500,
    "askVolume": 2.500,
    "totalVolume": 5.000
  }
}
```

---

## 📊 Tổng Hợp Endpoints

### Đã Triển Khai ✅

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/futures/balance` | Lấy số dư Futures | ✅ |
| POST | `/api/v1/futures/transfer` | Chuyển tiền vào/ra Futures | ✅ |
| POST | `/api/v1/futures/order` | Đặt lệnh | ✅ |
| GET | `/api/v1/futures/positions` | Lấy danh sách vị thế | ✅ |
| POST | `/api/v1/futures/position/close` | Đóng vị thế | ✅ |
| POST | `/api/v1/futures/leverage` | Điều chỉnh đòn bẩy | ✅ |
| GET | `/api/v1/futures/coins` | Lấy danh sách coin | ❌ |
| POST | `/api/v1/futuresKline/symbol` | Lấy dữ liệu Kline | ❌ |

### Chưa Triển Khai ⚠️

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| DELETE | `/api/v1/futures/order/{id}` | Hủy lệnh | ✅ |
| GET | `/api/v1/futures/orders` | Lấy danh sách lệnh | ✅ |
| GET | `/api/v1/futures/orderbook/{symbol}` | Lấy Order Book | ❌ |
| GET | `/api/v1/futures/trades` | Lịch sử giao dịch | ✅ |
| GET | `/api/v1/futures/trades/{symbol}` | Lịch sử giao dịch theo symbol | ❌ |

---

## 🔄 WebSocket Endpoints ⚠️ (Chưa triển khai)

### 1. Order Book Updates
```
WS /ws/futures/orderbook
```

**Subscribe Message:**
```json
{
  "method": "SUBSCRIBE",
  "params": ["btcusdt@depth"],
  "id": 1
}
```

**Update Message:**
```json
{
  "e": "depthUpdate",
  "E": 1701432000000,
  "s": "BTCUSDT",
  "b": [["44950.00", "0.500"]],
  "a": [["45000.00", "0.300"]]
}
```

---

### 2. Kline Updates
```
WS /ws/futures/klines
```

**Subscribe Message:**
```json
{
  "method": "SUBSCRIBE",
  "params": ["btcusdt@kline_1m"],
  "id": 1
}
```

---

### 3. User Data Stream
```
WS /ws/futures/user
```

**Updates:**
- Order updates
- Position updates
- Balance updates

---

## 📝 Request/Response Examples

### cURL Examples

**1. Đặt lệnh Market:**
```bash
curl -X POST http://localhost:8000/api/v1/futures/order \
  -H "Authorization: Bearer eyJhbGc..." \
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

**2. Lấy vị thế:**
```bash
curl -X GET http://localhost:8000/api/v1/futures/positions \
  -H "Authorization: Bearer eyJhbGc..."
```

**3. Đóng vị thế:**
```bash
curl -X POST http://localhost:8000/api/v1/futures/position/close \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{"symbol": "BTCUSDT"}'
```

---

### JavaScript Examples

**1. Đặt lệnh:**
```javascript
const placeOrder = async (orderData) => {
  const response = await fetch('http://localhost:8000/api/v1/futures/order', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(orderData)
  });
  
  return await response.json();
};

// Sử dụng
const order = await placeOrder({
  symbol: 'BTCUSDT',
  side: 'BUY',
  positionSide: 'LONG',
  type: 'MARKET',
  quantity: 0.1,
  leverage: 10
});
```

**2. Lấy Order Book:**
```javascript
const getOrderBook = async (symbol, limit = 20) => {
  const response = await fetch(
    `http://localhost:8000/api/v1/futures/orderbook/${symbol}?limit=${limit}`
  );
  return await response.json();
};

const orderBook = await getOrderBook('BTCUSDT', 20);
```

---

## 🚨 Error Codes

| HTTP Code | Meaning | Example |
|-----------|---------|---------|
| 200 | Success | Request thành công |
| 400 | Bad Request | Tham số không hợp lệ |
| 401 | Unauthorized | Token không hợp lệ hoặc hết hạn |
| 403 | Forbidden | Không có quyền truy cập |
| 404 | Not Found | Resource không tồn tại |
| 500 | Internal Server Error | Lỗi server |

---

## 📌 Rate Limits

- **Authenticated endpoints**: 1200 requests/phút
- **Public endpoints**: 2400 requests/phút
- **WebSocket connections**: 5 connections/IP

---

## 🔗 Tài Liệu Liên Quan

- [Futures API README](./FUTURES_API_README.md)
- [Futures Order API](./FUTURES_ORDER_API.md)
- [Futures Order Book](./FUTURES_ORDERBOOK.md)
- [Futures Kline API](./FUTURES_KLINE_API.md)

---

**Version**: 1.0  
**Last Updated**: 2025-12-01  
**Author**: API Exchange Development Team
