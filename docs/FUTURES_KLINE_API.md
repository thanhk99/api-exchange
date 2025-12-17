# 📊 API Futures Kline - Hướng dẫn sử dụng

## API Endpoint

```
POST /api/v1/futuresKline/symbol
```

## Request

### Headers
```
Content-Type: application/json
```

### Body
```json
{
  "symbol": "BTCUSDT",
  "interval": "1m"
}
```

### Query Parameters
- `limit` (optional, default: 72) - Số lượng kline cần lấy
  - Ví dụ: `?limit=288` để lấy 288 kline

## Response Structure

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
      "openPrice": 96500.50,
      "closePrice": 96520.30,
      "highPrice": 96550.00,
      "lowPrice": 96480.00,
      "volume": 125.45,
      "startTime": 1733043600000,
      "closeTime": 1733043659999,
      "interval": "1m",
      "isClosed": true
    }
  ]
}
```

### Data Fields

| Field | Type | Description |
|-------|------|-------------|
| `symbol` | String | Cặp giao dịch (e.g., "BTCUSDT") |
| `openPrice` | BigDecimal | Giá mở cửa |
| `closePrice` | BigDecimal | Giá đóng cửa |
| `highPrice` | BigDecimal | Giá cao nhất trong khoảng thời gian |
| `lowPrice` | BigDecimal | Giá thấp nhất trong khoảng thời gian |
| `volume` | BigDecimal | Khối lượng giao dịch |
| `startTime` | long | Timestamp bắt đầu (milliseconds) |
| `closeTime` | long | Timestamp kết thúc (milliseconds) |
| `interval` | String | Khoảng thời gian ("1m", "5m", "1h"...) |
| `isClosed` | boolean | Nến đã đóng (true) hay đang mở (false) |

## Supported Intervals

| Interval | Description | Max Limit |
|----------|-------------|-----------|
| `1m` | 1 phút | 500 |
| `5m` | 5 phút | 100 (500 nến 1m) |
| `15m` | 15 phút | 33 (500 nến 1m) |
| `1h` | 1 giờ | 500 |
| `6h` | 6 giờ | 83 (500 nến 1h) |
| `12h` | 12 giờ | 41 (500 nến 1h) |

## Examples

### 1. Lấy 288 nến 1m cho BTCUSDT

**cURL:**
```bash
curl -X POST "http://localhost:8000/api/v1/futuresKline/symbol?limit=288" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"BTCUSDT","interval":"1m"}'
```

**JavaScript (Fetch):**
```javascript
const response = await fetch('http://localhost:8000/api/v1/futuresKline/symbol?limit=288', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    symbol: 'BTCUSDT',
    interval: '1m'
  })
});

const data = await response.json();
console.log(`Received ${data.count} klines`);
```

### 2. Lấy 288 nến 1h cho ETHUSDT

**cURL:**
```bash
curl -X POST "http://localhost:8000/api/v1/futuresKline/symbol?limit=288" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"ETHUSDT","interval":"1h"}'
```

### 3. Lấy 100 nến 5m cho BNBUSDT

**cURL:**
```bash
curl -X POST "http://localhost:8000/api/v1/futuresKline/symbol?limit=100" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"BNBUSDT","interval":"5m"}'
```

## Usage for Chart

```javascript
async function fetchKlineData(symbol, interval, limit = 288) {
  const response = await fetch(
    `http://localhost:8000/api/v1/futuresKline/symbol?limit=${limit}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ symbol, interval })
    }
  );
  
  const result = await response.json();
  
  if (result.success) {
    return result.data.map(kline => ({
      time: kline.startTime,
      open: parseFloat(kline.openPrice),
      high: parseFloat(kline.highPrice),
      low: parseFloat(kline.lowPrice),
      close: parseFloat(kline.closePrice),
      volume: parseFloat(kline.volume)
    }));
  }
  
  throw new Error(result.message);
}

// Sử dụng
const chartData = await fetchKlineData('BTCUSDT', '1m', 288);
console.log(`Loaded ${chartData.length} candles for chart`);
```

## Notes

- **Limit linh hoạt**: Frontend có thể yêu cầu bất kỳ số lượng kline nào (tối đa 500)
- **Tính toán động**: Các interval như 5m, 15m được tính từ dữ liệu 1m
- **Real-time**: Scheduler tự động cập nhật dữ liệu mỗi phút (1m) và mỗi giờ (1h)
- **Thứ tự**: Dữ liệu trả về theo thứ tự mới nhất → cũ nhất (DESC)
