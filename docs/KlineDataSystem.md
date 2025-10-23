# Hệ thống Kline Data với Database Storage

## Tổng quan

Hệ thống này được thiết kế để lưu trữ và tính toán dữ liệu kline từ Binance API với các khoảng thời gian khác nhau. Dữ liệu được lưu trữ trong database và tính toán các khoảng thời gian phức tạp từ dữ liệu cơ bản.

## Kiến trúc hệ thống

### 1. Database Tables

#### Bảng `spot_kline_data_1m`
- Lưu trữ dữ liệu kline 1 phút
- Được sử dụng để tính toán các khoảng 5m, 15m
- Cập nhật mỗi phút

#### Bảng `spot_kline_data_1h`
- Lưu trữ dữ liệu kline 1 giờ
- Được sử dụng để tính toán các khoảng 6h, 12h
- Cập nhật mỗi giờ

### 2. Các thành phần chính

#### Models
- `SpotKlineData1m`: Model cho dữ liệu kline 1m
- `SpotKlineData1h`: Model cho dữ liệu kline 1h

#### Repositories
- `SpotKlineData1mRepository`: Repository cho dữ liệu 1m
- `SpotKlineData1hRepository`: Repository cho dữ liệu 1h

#### Services
- `BinanceKlineService`: Lấy dữ liệu từ Binance API
- `KlineCalculationService`: Tính toán các khoảng thời gian từ dữ liệu cơ bản

#### Scheduler
- `KlineDataScheduler`: Tự động lấy dữ liệu theo lịch trình

#### Controller
- `KlineDataController`: API REST để truy xuất dữ liệu

## Cách hoạt động

### 1. Thu thập dữ liệu
- **Mỗi phút**: Lấy dữ liệu kline 1m từ Binance API và lưu vào bảng `spot_kline_data_1m`
- **Mỗi giờ**: Lấy dữ liệu kline 1h từ Binance API và lưu vào bảng `spot_kline_data_1h`

### 2. Tính toán dữ liệu
- **5m**: Tính từ 5 nến 1m liên tiếp
- **15m**: Tính từ 15 nến 1m liên tiếp
- **6h**: Tính từ 6 nến 1h liên tiếp
- **12h**: Tính từ 12 nến 1h liên tiếp

### 3. Công thức tính toán
```
Open Price = Giá mở của nến đầu tiên
Close Price = Giá đóng của nến cuối cùng
High Price = Giá cao nhất trong tất cả các nến
Low Price = Giá thấp nhất trong tất cả các nến
Volume = Tổng volume của tất cả các nến
```

## API Endpoints

### 1. Lấy dữ liệu kline
```
GET /api/kline/{symbol}/{interval}?limit=72
```

**Ví dụ:**
```
GET /api/kline/BTCUSDT/5m?limit=72
GET /api/kline/ETHUSDT/15m?limit=50
GET /api/kline/SOLUSDT/6h?limit=72
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "interval": "5m",
  "data": [
    {
      "symbol": "BTCUSDT",
      "openPrice": 45000.00,
      "closePrice": 45100.00,
      "highPrice": 45200.00,
      "lowPrice": 44900.00,
      "volume": 100.5,
      "startTime": 1640995200000,
      "closeTime": 1640995500000,
      "interval": "5m",
      "isClosed": true
    }
  ],
  "count": 72,
  "limit": 72,
  "success": true,
  "message": "Dữ liệu kline được lấy thành công"
}
```

### 2. Lấy danh sách khoảng thời gian được hỗ trợ
```
GET /api/kline/intervals
```

### 3. Lấy danh sách symbols được hỗ trợ
```
GET /api/kline/symbols
```

### 4. Lấy thông tin chi tiết
```
GET /api/kline/{symbol}/{interval}/info
```

### 5. Lấy dữ liệu cho tất cả symbols
```
GET /api/kline/all/{interval}?limit=72
```

## Khoảng thời gian được hỗ trợ

| Interval | Nguồn dữ liệu | Mô tả |
|----------|---------------|-------|
| 1m | Binance API | Dữ liệu trực tiếp từ Binance |
| 5m | Tính từ 1m | 5 nến 1m liên tiếp |
| 15m | Tính từ 1m | 15 nến 1m liên tiếp |
| 1h | Binance API | Dữ liệu trực tiếp từ Binance |
| 6h | Tính từ 1h | 6 nến 1h liên tiếp |
| 12h | Tính từ 1h | 12 nến 1h liên tiếp |

## Symbols được theo dõi

- BTCUSDT
- ETHUSDT
- SOLUSDT

## Lịch trình tự động

- **Mỗi phút (0 * * * * *)**: Lấy dữ liệu 1m
- **Mỗi giờ (0 0 * * * *)**: Lấy dữ liệu 1h
- **Khởi động ứng dụng**: Lấy dữ liệu ban đầu sau 30 giây
- **Hàng ngày lúc 2:00 AM**: Dọn dẹp dữ liệu cũ

## Lưu ý quan trọng

1. **Dữ liệu 1s**: Vẫn được xử lý riêng biệt qua SpotPriceCoinSocket và RingBufferService
2. **Tính toán realtime**: Các khoảng 5m, 15m, 6h, 12h được tính toán realtime từ dữ liệu cơ bản
3. **Performance**: Dữ liệu được lưu trữ trong database để đảm bảo persistence và có thể truy xuất nhanh
4. **Scalability**: Có thể dễ dàng thêm symbols và khoảng thời gian mới
5. **Data retention**: Dữ liệu 1m được giữ 7 ngày, dữ liệu 1h được giữ 30 ngày
