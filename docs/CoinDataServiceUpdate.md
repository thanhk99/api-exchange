# Cập nhật hệ thống CoinDataService

## Tổng quan

Đã gộp `SpotCoinKlineService` và `CoinDataService` thành một service duy nhất và cập nhật để sử dụng CoinGecko API làm nguồn dữ liệu chính.

## Thay đổi chính

### 1. Gộp Services
- **Trước**: Có 2 services riêng biệt:
  - `SpotCoinKlineService`: Xử lý kline data từ Binance
  - `CoinDataService`: Xử lý coin info và price history
- **Sau**: Chỉ còn 1 service `CoinDataService` tích hợp tất cả chức năng

### 2. Chuyển đổi API
- **Trước**: Chỉ sử dụng Binance API
- **Sau**: Sử dụng CoinGecko API làm chính, Binance API làm fallback

### 3. Mapping Coin IDs
```java
private static final Map<String, String> COIN_ID_TO_SYMBOL = Map.of(
    "bitcoin", "BTCUSDT",
    "ethereum", "ETHUSDT", 
    "solana", "SOLUSDT"
);
```

## Chức năng mới

### 1. CoinGecko API Integration
- Lấy dữ liệu kline từ CoinGecko API
- Lấy thông tin coin từ CoinGecko API
- Fallback sang Binance API nếu CoinGecko không có dữ liệu

### 2. Dual API Support
```java
// Thử CoinGecko trước
List<KlinesSpotResponse> klines = fetchKlineDataFromCoinGecko(coinId, "1m", 72);

// Nếu CoinGecko không có dữ liệu, fallback sang Binance
if (klines.isEmpty()) {
    klines = fetchKlineDataFromBinance(symbol, "1m", 72);
}
```

### 3. Enhanced Coin Info
- Lấy thông tin chi tiết từ CoinGecko
- Bao gồm market data, price changes, etc.
- Cập nhật mỗi 10 giây

## API Endpoints CoinGecko

### 1. Market Chart Data
```
GET /coins/{coinId}/market_chart?vs_currency=usd&days={days}&interval={interval}
```

### 2. Coin Information
```
GET /coins/{coinId}?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false
```

## Cấu hình

### application.properties
```properties
# CoinGecko API
coingecko.api.url=https://api.coingecko.com/api/v3

# Binance API (fallback)
binance.api.url=https://api.binance.com
```

## Lịch trình tự động

- **Mỗi phút**: Lấy dữ liệu kline 1m từ CoinGecko/Binance
- **Mỗi giờ**: Lấy dữ liệu kline 1h từ CoinGecko/Binance
- **Mỗi 10 giây**: Cập nhật thông tin coin từ CoinGecko
- **Khởi động**: Lấy dữ liệu ban đầu sau 30 giây

## Lợi ích

1. **Đa dạng nguồn dữ liệu**: Không phụ thuộc vào một API duy nhất
2. **Tính sẵn sàng cao**: Fallback mechanism đảm bảo dữ liệu luôn có sẵn
3. **Dữ liệu phong phú**: CoinGecko cung cấp nhiều thông tin hơn Binance
4. **Dễ bảo trì**: Chỉ cần quản lý một service duy nhất
5. **Hiệu suất tốt**: Cache và optimization được tích hợp

## Lưu ý

- CoinGecko có rate limit, cần quản lý cẩn thận
- Binance API vẫn được sử dụng làm fallback
- Dữ liệu từ CoinGecko có thể khác với Binance về format
- Cần monitor cả hai API để đảm bảo tính sẵn sàng
