# Spot & Market (Public) API Documentation

This documentation covers the public endpoints for Spot trading and general Market data, following the standard format.

## Base URL
`http://[host]:8000/api/v1/public`

---

## 1. Public Spot Data (`/spot`)

### Get Order Book
- **Endpoint**: `GET /spot/orderbook/{symbol}`
- **Description**: Returns the combined order book (bids and asks) for a specific symbol.
- **Parameters**:
  - `limit` (Query, Optional): Maximum number of entries. Default: 20.
- **Example**: `/api/v1/public/spot/orderbook/BTCUSDT?limit=10`

### Get Kline Data
- **Endpoint**: `POST /spot/kline`
- **Description**: Fetch historical/standard candlestick data for a symbol.
- **Parameters**:
  - `limit` (Query, Optional): Number of candles. Default: 500.
- **Body**:
  ```json
  {
    "symbol": "BTCUSDT",
    "interval": "1m"
  }
  ```
- **Supported Intervals**: `1s`, `1m`, `5m`, `15m`, `1h`, `6h`, `12h`.

### Get Real-time Kline (RingBuffer)
- **Endpoint**: `POST /spot/kline/realtime`
- **Description**: Fast access to the most recent candles from memory (last 72).
- **Body**:
  ```json
  {
    "symbol": "BTCUSDT"
  }
  ```

### Get Supported Intervals
- **Endpoint**: `GET /spot/kline/intervals`

### Get Supported Symbols
- **Endpoint**: `GET /spot/kline/symbols`

---

## 2. Public Market Data (`/coin`)

### Get All Coins List
- **Endpoint**: `GET /coin/list`
- **Description**: Returns basic market info (price, change, volume) for all supported coins.

### Get Exchange Rate
- **Endpoint**: `GET /coin/exchange-rate`
- **Description**: Get the conversion rate between two assets.
- **Parameters**:
  - `from` (Query, Required): Source asset symbol.
  - `to` (Query, Required): Target asset symbol.
- **Example**: `/api/v1/public/coin/exchange-rate?from=BTC&to=USDT`
