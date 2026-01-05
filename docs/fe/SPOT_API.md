# Spot Trading API Documentation

## Base URL
`http://[host]:8000`

---

## 1. Public Market Data (`/api/v1/public/spot`)
*No token required*

### Order Book
- **GET** `/api/v1/public/spot/orderbook/{symbol}`
- **Params:** `limit` (default: 20)
- **Example:** `/api/v1/public/spot/orderbook/BTCUSDT?limit=10`

### Historical Kline Data
- **POST** `/api/v1/public/spot/kline`
- **Body:**
```json
{
  "symbol": "BTCUSDT",
  "interval": "1m"
}
```
- **Intervals:** `1m`, `5m`, `15m`, `1h`, `6h`, `12h`

### Realtime Kline (RingBuffer)
- **POST** `/api/v1/public/spot/kline/realtime`
- **Body:** `{"symbol": "BTCUSDT"}`
- **Note:** Returns the last 72 candles directly from memory.

### Supported Symbols & Intervals
- **GET** `/api/v1/public/spot/kline/symbols`
- **GET** `/api/v1/public/spot/kline/intervals`

---

## 2. Trading Operations (`/api/v1/spot`)
*Requires Header: `Authorization: Bearer <token>`*

### Create Order (Buy/Sell)
- **POST** `/api/v1/spot/create`
- **Body:**
```json
{
  "symbol": "BTCUSDT",
  "price": 45000,
  "quantity": 0.001,
  "type": "LIMIT",
  "side": "BUY"
}
```

### Cancel Order
- **POST** `/api/v1/spot/cancle`
- **Body:** `{"id": 12345}`

---

## 3. WebSocket (Spot)
- **Endpoint:** `ws://[host]:8000/ws`

| Topic | Description |
|-------|-------------|
| `/topic/spot-prices` | Real-time tickers for all coins |
| `/topic/kline-data` | Real-time 1s candlestick data |
