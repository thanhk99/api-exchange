# API & WebSocket Documentation for Frontend

## Base URL
`http://[host]:8000`

---

## 1. Authentication (`/api/v1/auth`)

### Signup
- **POST** `/api/v1/auth/signup`
- **Body:** `{"email": "...", "password": "...", "fullName": "..."}`

### Login
- **POST** `/api/v1/auth/login`
- **Body:** `{"email": "...", "password": "..."}`
- **Response:** `{"token": "...", "refreshToken": "...", "user": {...}}`

---

## 2. Public Market Data (`/api/v1/public`)

### Coin Data
- **GET** `/api/v1/public/coin/list`
  - Get all coin prices, market cap, and change %.
- **GET** `/api/v1/coin/markets` (Legacy support, Public)
  - Same as above, provided for backward compatibility.
- **GET** `/api/v1/public/coin/exchange-rate?from=BTC&to=USDT`
  - Get exchange rate between two symbols.

### Spot Data
- **GET** `/api/v1/public/spot/orderbook/{symbol}`
  - Example: `/api/v1/public/spot/orderbook/BTCUSDT?limit=20`
- **POST** `/api/v1/public/spot/kline`
  - **Body:** `{"symbol": "BTCUSDT", "interval": "1m"}`
- **POST** `/api/v1/public/spot/kline/realtime`
  - Uses RingBuffer for fast access (last 72 candles).
  - **Body:** `{"symbol": "BTCUSDT"}`

### Futures Data
- **GET** `/api/v1/public/futures/coins`
  - Get market overview for all futures symbols.
- **POST** `/api/v1/public/futures/kline`
  - **Body:** `{"symbol": "BTCUSDT", "interval": "1h"}`

---

## 3. Trading APIs (Private)
*Requires Header: `Authorization: Bearer <token>`*

### Spot Trading (`/api/v1/spot`)
- **POST** `/api/v1/spot/create` (Place Order)
- **POST** `/api/v1/spot/cancle` (Cancel Order)

### Futures Trading (`/api/v1/futures`)
- **GET** `/api/v1/futures/orders` (Order History)
- **POST** `/api/v1/futures/orders` (Place Order)
- **POST** `/api/v1/futures/positions/leverage` (Adjust Leverage)

---

## 4. WebSocket (STOMP)
- **Endpoint:** `ws://[host]:8000/ws`

### Topics
| Topic | Description | Format |
|-------|-------------|--------|
| `/topic/spot-prices` | Real-time prices for all symbols | JSON |
| `/topic/kline-data` | Real-time 1s Kline data | JSON |
| `/topic/futures/markets` | All futures market tickers | JSON |
| `/topic/futures/kline/1s/{symbol}` | 1s Kline per symbol | JSON |

### Binary Topic (Advanced)
- `/topic/spot-prices-binary`
- **Format:** Protobuf (Schema available in `MarketDataProto.java`)
