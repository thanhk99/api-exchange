# Kline Data API với RingBuffer

## Tổng quan

Hệ thống này tích hợp SpotPriceCoinSocket với RingBufferService để lưu trữ dữ liệu kline realtime với giới hạn 72 nến cho mỗi symbol và cung cấp API để truy xuất dữ liệu.

## Các thành phần chính

### 1. RingBufferService
- Quản lý dữ liệu kline với RingBuffer có giới hạn 72 nến cho mỗi symbol
- Thread-safe với ReadWriteLock
- Tự động ghi đè dữ liệu cũ khi buffer đầy

### 2. SpotPriceCoinSocket (Đã cập nhật)
- Tích hợp với RingBufferService
- Tự động lưu dữ liệu kline vào RingBuffer khi nhận được từ WebSocket
- Vẫn gửi dữ liệu qua WebSocket topic `/topic/kline-data`

### 3. KlineDataController
- API REST để truy xuất dữ liệu kline từ RingBuffer
- Cung cấp các endpoint để quản lý dữ liệu

## API Endpoints

### 1. Lấy dữ liệu kline của một symbol
```
GET /api/kline/{symbol}
```

**Ví dụ:**
```
GET /api/kline/BTCUSDT
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "data": [
    {
      "symbol": "BTCUSDT",
      "openPrice": 45000.00,
      "closePrice": 45100.00,
      "highPrice": 45200.00,
      "lowPrice": 44900.00,
      "volume": 100.5,
      "startTime": 1640995200000,
      "closeTime": 1640995260000,
      "interval": "1s",
      "isClosed": true
    }
  ],
  "count": 72,
  "maxSize": 72,
  "success": true,
  "message": "Dữ liệu kline được lấy thành công"
}
```

### 2. Lấy danh sách tất cả symbols có dữ liệu
```
GET /api/kline/symbols
```

**Response:**
```json
{
  "symbols": ["BTCUSDT", "ETHUSDT", "SOLUSDT"],
  "count": 3,
  "success": true,
  "message": "Danh sách symbols được lấy thành công"
}
```

### 3. Lấy thông tin chi tiết về buffer của một symbol
```
GET /api/kline/{symbol}/info
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "currentSize": 72,
  "maxSize": 72,
  "isFull": true,
  "isEmpty": false,
  "hasData": true,
  "latestKline": { ... },
  "oldestKline": { ... },
  "timeRange": {
    "startTime": 1640995200000,
    "endTime": 1640995260000
  },
  "success": true,
  "message": "Thông tin symbol được lấy thành công"
}
```

### 4. Xóa dữ liệu của một symbol
```
DELETE /api/kline/{symbol}
```

### 5. Xóa tất cả dữ liệu
```
DELETE /api/kline/clear-all
```

## Cách hoạt động

1. **SpotPriceCoinSocket** kết nối với Binance WebSocket và nhận dữ liệu kline realtime
2. Mỗi khi nhận được dữ liệu kline mới, nó sẽ:
   - Thêm dữ liệu vào **RingBufferService** cho symbol tương ứng
   - Gửi dữ liệu qua WebSocket topic `/topic/kline-data`
3. **RingBufferService** tự động quản lý buffer với giới hạn 72 nến cho mỗi symbol
4. **KlineDataController** cung cấp API REST để truy xuất dữ liệu từ RingBuffer

## Symbols được theo dõi

Hiện tại hệ thống theo dõi các symbols sau:
- BTCUSDT
- ETHUSDT  
- SOLUSDT

## Lưu ý

- Dữ liệu được lưu trữ trong memory, sẽ mất khi restart ứng dụng
- RingBuffer tự động ghi đè dữ liệu cũ khi đạt giới hạn 72 nến
- Tất cả operations đều thread-safe
- API hỗ trợ CORS cho tất cả origins
